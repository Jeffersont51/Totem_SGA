package br.com.jefferson.totemsga.ads;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.gson.Gson;

import br.com.jefferson.totemsga.R;
import br.com.jefferson.totemsga.util.SessionManager;

public class AdFragment extends Fragment {

    private StyledPlayerView playerView;
    private ImageView ivAdImage;
    private View flMarqueeContainer;
    private TextView tvMarquee;
    private View touchOverlay;
    
    private ExoPlayer exoPlayer;
    private final Gson gson = new Gson();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ad_display, container, false);
        playerView = view.findViewById(R.id.playerView);
        ivAdImage = view.findViewById(R.id.ivAdImage);
        flMarqueeContainer = view.findViewById(R.id.flMarqueeContainer);
        tvMarquee = view.findViewById(R.id.tvMarquee);
        touchOverlay = view.findViewById(R.id.touchOverlay);

        touchOverlay.setOnClickListener(v -> dismissAds());

        // Garantir que esta tela sequestre o foco do teclado
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        view.requestFocus();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadAd();
    }

    private void loadAd() {
        SessionManager sm = new SessionManager(requireContext());
        String url = sm.getAdsSingleUrl();
        String type = sm.getAdsSingleType();
        String bgColor = sm.getAdsSingleBgColor();

        applyLetreiro();

        // Aplicar cor de fundo na View raiz
        if (getView() != null) {
            try {
                getView().setBackgroundColor(android.graphics.Color.parseColor(bgColor));
            } catch (Exception e) {
                getView().setBackgroundColor(android.graphics.Color.BLACK);
            }
        }

        if (url == null || url.trim().isEmpty()) {
            showFallback();
            return;
        }

        resetViews();

        switch (type) {
            case "VIDEO":
                showVideo(url);
                break;
            case "GIF":
                showGif(url);
                break;
            case "IMAGE":
                showImage(url);
                break;
            default:
                showFallback();
                break;
        }
    }

    private void applyLetreiro() {
        SessionManager sm = new SessionManager(requireContext());
        br.com.jefferson.totemsga.model.LetreiroConfig config = gson.fromJson(sm.getLetreiroConfig(), br.com.jefferson.totemsga.model.LetreiroConfig.class);

        if (config == null || !config.habilitado) {
            flMarqueeContainer.setVisibility(View.GONE);
            return;
        }

        flMarqueeContainer.setVisibility(View.VISIBLE);
        tvMarquee.setText(config.mensagem);
        
        try {
            tvMarquee.setTextColor(android.graphics.Color.parseColor(config.corFonte));
        } catch (Exception e) {
            tvMarquee.setTextColor(android.graphics.Color.WHITE);
        }

        try {
            flMarqueeContainer.setBackgroundColor(android.graphics.Color.parseColor(config.corFundo));
        } catch (Exception e) {
            flMarqueeContainer.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        }

        tvMarquee.setTextSize(config.tamanhoFonte);

        int style = android.graphics.Typeface.NORMAL;
        if ("NEGRITO".equals(config.estilo)) style = android.graphics.Typeface.BOLD;
        else if ("ITALICO".equals(config.estilo)) style = android.graphics.Typeface.ITALIC;
        tvMarquee.setTypeface(null, style);

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) flMarqueeContainer.getLayoutParams();
        lp.width = FrameLayout.LayoutParams.MATCH_PARENT;
        
        if ("TOPO".equals(config.posicao)) {
            lp.gravity = android.view.Gravity.TOP;
        } else if ("CENTRO".equals(config.posicao)) {
            lp.gravity = android.view.Gravity.CENTER;
        } else {
            lp.gravity = android.view.Gravity.BOTTOM;
        }
        flMarqueeContainer.setLayoutParams(lp);

        applyAnimation(tvMarquee, config);
    }

    private void applyAnimation(TextView textView, br.com.jefferson.totemsga.model.LetreiroConfig config) {
        textView.clearAnimation();
        int velocidadeMs = config.velocidadeSegundos * 1000;
        
        if ("PISCAR".equals(config.efeito)) {
            AlphaAnimation blink = new AlphaAnimation(1.0f, 0.0f);
            blink.setDuration(velocidadeMs / 5); 
            blink.setRepeatCount(Animation.INFINITE);
            blink.setRepeatMode(Animation.REVERSE);
            textView.startAnimation(blink);
            
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) textView.getLayoutParams();
            lp.gravity = android.view.Gravity.CENTER;
            textView.setLayoutParams(lp);

        } else if ("DESLIZAR".equals(config.efeito)) {
            float fromX = 0, toX = 0, fromY = 0, toY = 0;
            
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) textView.getLayoutParams();
            lp.gravity = android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL;
            textView.setLayoutParams(lp);

            switch (config.direcao) {
                case "DIREITA_ESQUERDA": fromX = 1.0f; toX = -1.0f; break;
                case "ESQUERDA_DIREITA": fromX = -1.0f; toX = 1.0f; break;
                case "BAIXO_CIMA": fromY = 1.0f; toY = -1.0f; break;
                case "CIMA_BAIXO": fromY = -1.0f; toY = 1.0f; break;
            }

            TranslateAnimation anim = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, fromX,
                Animation.RELATIVE_TO_PARENT, toX,
                Animation.RELATIVE_TO_PARENT, fromY,
                Animation.RELATIVE_TO_PARENT, toY
            );
            anim.setDuration(velocidadeMs);
            anim.setRepeatCount(Animation.INFINITE);
            anim.setRepeatMode(Animation.RESTART);
            textView.startAnimation(anim);
        } else {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) textView.getLayoutParams();
            lp.gravity = android.view.Gravity.CENTER;
            textView.setLayoutParams(lp);
        }
    }

    private void resetViews() {
        playerView.setVisibility(View.GONE);
        ivAdImage.setVisibility(View.GONE);
        if (exoPlayer != null) {
            exoPlayer.stop();
        }
    }

    private void showVideo(String url) {
        playerView.setVisibility(View.VISIBLE);
        if (exoPlayer == null) {
            exoPlayer = new ExoPlayer.Builder(requireContext()).build();
            playerView.setPlayer(exoPlayer);
            exoPlayer.setRepeatMode(Player.REPEAT_MODE_ALL); // Loop Infinito
        }
        MediaItem mediaItem = MediaItem.fromUri(url);
        exoPlayer.setMediaItem(mediaItem);
        exoPlayer.prepare();
        exoPlayer.play();
    }

    private void showGif(String url) {
        ivAdImage.setVisibility(View.VISIBLE);
        Glide.with(this)
                .asGif()
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(ivAdImage);
    }

    private void showImage(String url) {
        ivAdImage.setVisibility(View.VISIBLE);
        Glide.with(this)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(ivAdImage);
    }

    private void showFallback() {
        resetViews();
        ivAdImage.setVisibility(View.VISIBLE);
        ivAdImage.setImageResource(R.drawable.ic_launcher_legacy); // Ou sua logo padrão
    }

    private void dismissAds() {
        if (getActivity() instanceof br.com.jefferson.totemsga.MainActivity) {
            br.com.jefferson.totemsga.MainActivity activity = (br.com.jefferson.totemsga.MainActivity) getActivity();
            AdManager.getInstance().onAdDismissed();
            activity.startFlow();
        }
        getParentFragmentManager().beginTransaction().remove(this).commit();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }
}
