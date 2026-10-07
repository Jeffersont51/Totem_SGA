package br.com.jefferson.totemsga.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.SmbConfig;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Atualização do app a partir de uma pasta compartilhada do Windows (SMB),
 * ex.: \\servidor\Programas TI\TOTEM_SGA. Procura o APK de maior versão no
 * nome do arquivo (TOTEM_SGA_v1.2.3.apk), baixa e confere antes de instalar.
 * Todos os métodos fazem rede/disco: chamar fora da thread de UI.
 */
public class AppUpdater {

    private static final Pattern VERSION_IN_NAME = Pattern.compile("(\\d+(?:\\.\\d+)+)");

    public static class Found {
        public String fileName;
        public String version;
        public long size;
    }

    public interface Progress {
        void onProgress(int percent);
    }

    private static class Location {
        String host;
        String share;
        String path;
    }

    private static Location parse(String unc) throws Exception {
        String s = unc == null ? "" : unc.trim().replace('/', '\\');
        while (s.startsWith("\\")) s = s.substring(1);
        String[] parts = s.split("\\\\+");
        if (parts.length < 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
            throw new Exception("Caminho de rede inválido. Use o formato \\\\servidor\\pasta");
        }
        Location loc = new Location();
        loc.host = parts[0];
        loc.share = parts[1];
        StringBuilder sb = new StringBuilder();
        for (int i = 2; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            if (sb.length() > 0) sb.append("\\");
            sb.append(parts[i]);
        }
        loc.path = sb.toString();
        return loc;
    }

    // Aceita "usuario", "DOMINIO\\usuario" ou "usuario@dominio"; vazio = convidado
    private static AuthenticationContext auth(String user, String pass) {
        String u = user == null ? "" : user.trim();
        if (u.isEmpty()) return AuthenticationContext.guest();
        String domain = "";
        int slash = u.indexOf('\\');
        int at = u.indexOf('@');
        if (slash > 0) {
            domain = u.substring(0, slash);
            u = u.substring(slash + 1);
        } else if (at > 0) {
            domain = u.substring(at + 1);
            u = u.substring(0, at);
        }
        return new AuthenticationContext(u, (pass == null ? "" : pass).toCharArray(), domain);
    }

    private static SMBClient newClient() {
        return new SMBClient(SmbConfig.builder()
                .withTimeout(20, TimeUnit.SECONDS)
                .withSoTimeout(30, TimeUnit.SECONDS)
                .build());
    }

    /** Devolve o APK de maior versão na pasta, ou null se não houver nenhum APK. */
    public static Found findLatest(String unc, String user, String pass) throws Exception {
        Location loc = parse(unc);
        SMBClient client = newClient();
        try (Connection connection = client.connect(loc.host);
             Session session = connection.authenticate(auth(user, pass));
             DiskShare share = (DiskShare) session.connectShare(loc.share)) {

            Found best = null;
            for (FileIdBothDirectoryInformation f : share.list(loc.path, "*.apk")) {
                String name = f.getFileName();
                if (name == null || !name.toLowerCase().endsWith(".apk")) continue;
                Found candidate = new Found();
                candidate.fileName = name;
                candidate.version = versionFromName(name);
                candidate.size = f.getEndOfFile();
                if (best == null || compareVersions(candidate.version, best.version) > 0) {
                    best = candidate;
                }
            }
            return best;
        } finally {
            client.close();
        }
    }

    /** Baixa o APK para a pasta de atualizações do app e devolve o arquivo local. */
    public static File download(Context context, String unc, String user, String pass, Found found, Progress progress) throws Exception {
        Location loc = parse(unc);
        File dir = new File(context.getExternalFilesDir(null), "updates");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new Exception("Não foi possível criar a pasta de atualização no aparelho.");
        }
        // Remove downloads antigos para não acumular
        File[] old = dir.listFiles();
        if (old != null) for (File o : old) o.delete();

        File dest = new File(dir, "update.apk");
        String remotePath = loc.path.isEmpty() ? found.fileName : loc.path + "\\" + found.fileName;

        SMBClient client = newClient();
        try (Connection connection = client.connect(loc.host);
             Session session = connection.authenticate(auth(user, pass));
             DiskShare share = (DiskShare) session.connectShare(loc.share);
             com.hierynomus.smbj.share.File remote = share.openFile(remotePath,
                     EnumSet.of(AccessMask.GENERIC_READ), null, SMB2ShareAccess.ALL,
                     SMB2CreateDisposition.FILE_OPEN, null);
             InputStream in = remote.getInputStream();
             OutputStream out = new FileOutputStream(dest)) {

            byte[] buffer = new byte[64 * 1024];
            long total = 0;
            int lastPercent = -1;
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                total += read;
                if (progress != null && found.size > 0) {
                    int percent = (int) (total * 100 / found.size);
                    if (percent != lastPercent) {
                        lastPercent = percent;
                        progress.onProgress(percent);
                    }
                }
            }
            out.flush();
        } finally {
            client.close();
        }

        if (found.size > 0 && dest.length() != found.size) {
            dest.delete();
            throw new Exception("Download incompleto. Tente novamente.");
        }
        return dest;
    }

    /**
     * Confere se o APK baixado é mesmo este app, com a mesma assinatura e versão
     * igual ou maior. Devolve null se estiver tudo certo, ou o motivo da recusa.
     */
    @SuppressWarnings("deprecation")
    public static String validate(Context context, File apk) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo archive = pm.getPackageArchiveInfo(apk.getAbsolutePath(), PackageManager.GET_SIGNATURES);
            if (archive == null) return "O arquivo baixado não é um APK válido.";
            if (!context.getPackageName().equals(archive.packageName)) {
                return "O APK da pasta não é o Totem SGA (" + archive.packageName + ").";
            }
            PackageInfo installed = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
            if (archive.versionCode < installed.versionCode) {
                return "O APK da pasta (" + archive.versionName + ") é mais antigo que o instalado (" + installed.versionName + ").";
            }
            if (!sameSignatures(archive.signatures, installed.signatures)) {
                return "O APK da pasta foi assinado com outra chave e não pode ser instalado por cima deste.";
            }
            return null;
        } catch (Exception e) {
            return "Não foi possível conferir o APK: " + e.getMessage();
        }
    }

    private static boolean sameSignatures(Signature[] a, Signature[] b) {
        if (a == null || b == null || a.length == 0 || a.length != b.length) return false;
        String[] sa = new String[a.length];
        String[] sb = new String[b.length];
        for (int i = 0; i < a.length; i++) sa[i] = a[i].toCharsString();
        for (int i = 0; i < b.length; i++) sb[i] = b[i].toCharsString();
        Arrays.sort(sa);
        Arrays.sort(sb);
        return Arrays.equals(sa, sb);
    }

    public static String versionFromName(String fileName) {
        Matcher m = VERSION_IN_NAME.matcher(fileName);
        return m.find() ? m.group(1) : "0";
    }

    /** Compara versões no formato 1.2.3; devolve >0 se a for maior que b. */
    public static int compareVersions(String a, String b) {
        String[] pa = (a == null ? "0" : a).split("\\.");
        String[] pb = (b == null ? "0" : b).split("\\.");
        int n = Math.max(pa.length, pb.length);
        for (int i = 0; i < n; i++) {
            int va = i < pa.length ? parseIntSafe(pa[i]) : 0;
            int vb = i < pb.length ? parseIntSafe(pb[i]) : 0;
            if (va != vb) return va - vb;
        }
        return 0;
    }

    private static int parseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }
}
