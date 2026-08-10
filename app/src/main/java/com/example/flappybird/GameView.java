package com.example.flappybird;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class GameView extends SurfaceView implements Runnable {

    private Thread gameThread;
    private volatile boolean isPlaying;
    private SurfaceHolder holder;
    private Paint paint;

    // Bird
    private float birdX, birdY;
    private float birdVelocity;
    private static final float GRAVITY = 0.6f;
    private static final float JUMP_STRENGTH = -12f;
    private static final float BIRD_SIZE = 60f;

    // Pipes
    private ArrayList<Pipe> pipes;
    private static final float PIPE_WIDTH = 150f;
    private static final float PIPE_GAP = 400f;
    private static final float PIPE_SPEED = 8f;
    private long lastPipeTime;
    private static final long PIPE_INTERVAL = 1800; // ms

    // Score & state
    private int score = 0;
    private boolean gameOver = false;
    private boolean started = false;
    private Random random = new Random();

    // Screen
    private int screenWidth, screenHeight;

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        holder = getHolder();
        paint = new Paint();
        paint.setAntiAlias(true);
        pipes = new ArrayList<>();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenWidth = w;
        screenHeight = h;
        resetGame();
    }

    private void resetGame() {
        birdX = screenWidth / 4f;
        birdY = screenHeight / 2f;
        birdVelocity = 0;
        pipes.clear();
        score = 0;
        gameOver = false;
        started = false;
        lastPipeTime = System.currentTimeMillis();
    }

    @Override
    public void run() {
        while (isPlaying) {
            long startTime = System.currentTimeMillis();
            update();
            draw();
            long timeTaken = System.currentTimeMillis() - startTime;
            if (timeTaken < 16) { // ~60 FPS
                try {
                    Thread.sleep(16 - timeTaken);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void update() {
        if (!started || gameOver) return;

        // Bird physics
        birdVelocity += GRAVITY;
        birdY += birdVelocity;

        // Ground / ceiling collision
        if (birdY + BIRD_SIZE / 2 > screenHeight - 50 || birdY - BIRD_SIZE / 2 < 0) {
            gameOver = true;
            return;
        }

        // Spawn pipes
        long now = System.currentTimeMillis();
        if (now - lastPipeTime > PIPE_INTERVAL) {
            float gapY = 200 + random.nextFloat() * (screenHeight - 500);
            pipes.add(new Pipe(screenWidth, gapY));
            lastPipeTime = now;
        }

        // Move pipes & check collision / score
        Iterator<Pipe> it = pipes.iterator();
        while (it.hasNext()) {
            Pipe p = it.next();
            p.x -= PIPE_SPEED;

            if (p.x + PIPE_WIDTH < 0) {
                it.remove();
                continue;
            }

            // Score
            if (!p.scored && p.x + PIPE_WIDTH < birdX) {
                score++;
                p.scored = true;
            }

            // Collision
            if (RectF.intersects(getBirdRect(), p.getTopRect()) ||
                RectF.intersects(getBirdRect(), p.getBottomRect())) {
                gameOver = true;
            }
        }
    }

    private RectF getBirdRect() {
        return new RectF(birdX - BIRD_SIZE / 2, birdY - BIRD_SIZE / 2,
                         birdX + BIRD_SIZE / 2, birdY + BIRD_SIZE / 2);
    }

    private void draw() {
        if (!holder.getSurface().isValid()) return;

        Canvas canvas = holder.lockCanvas();
        if (canvas == null) return;

        // Background
        canvas.drawColor(Color.parseColor("#70C5CE"));

        // Ground
        paint.setColor(Color.parseColor("#DED895"));
        canvas.drawRect(0, screenHeight - 50, screenWidth, screenHeight, paint);
        paint.setColor(Color.parseColor("#D4A017"));
        canvas.drawRect(0, screenHeight - 60, screenWidth, screenHeight - 50, paint);

        // Pipes
        paint.setColor(Color.parseColor("#73BF2E"));
        for (Pipe p : pipes) {
            // Top pipe
            canvas.drawRect(p.getTopRect(), paint);
            // Bottom pipe
            canvas.drawRect(p.getBottomRect(), paint);
            // Pipe caps
            paint.setColor(Color.parseColor("#558B2F"));
            canvas.drawRect(p.x - 10, p.gapY - PIPE_GAP / 2 - 30, p.x + PIPE_WIDTH + 10, p.gapY - PIPE_GAP / 2, paint);
            canvas.drawRect(p.x - 10, p.gapY + PIPE_GAP / 2, p.x + PIPE_WIDTH + 10, p.gapY + PIPE_GAP / 2 + 30, paint);
            paint.setColor(Color.parseColor("#73BF2E"));
        }

        // Bird (simple circle + eye)
        paint.setColor(Color.parseColor("#F7DC6F"));
        canvas.drawCircle(birdX, birdY, BIRD_SIZE / 2, paint);
        paint.setColor(Color.BLACK);
        canvas.drawCircle(birdX + 15, birdY - 10, 8, paint); // eye
        paint.setColor(Color.parseColor("#E67E22"));
        // beak
        canvas.drawRect(birdX + 20, birdY - 5, birdX + 40, birdY + 5, paint);

        // Score
        paint.setColor(Color.WHITE);
        paint.setTextSize(80);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(String.valueOf(score), screenWidth / 2f, 120, paint);

        // Messages
        paint.setTextSize(60);
        if (!started) {
            canvas.drawText("TAP TO START", screenWidth / 2f, screenHeight / 2f - 50, paint);
            paint.setTextSize(40);
            canvas.drawText("Flappy Bird Clone", screenWidth / 2f, screenHeight / 2f + 30, paint);
        } else if (gameOver) {
            canvas.drawText("GAME OVER", screenWidth / 2f, screenHeight / 2f - 50, paint);
            paint.setTextSize(40);
            canvas.drawText("Tap to Restart", screenWidth / 2f, screenHeight / 2f + 30, paint);
            canvas.drawText("Score: " + score, screenWidth / 2f, screenHeight / 2f + 90, paint);
        }

        holder.unlockCanvasAndPost(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (gameOver) {
                resetGame();
            } else if (!started) {
                started = true;
            }
            birdVelocity = JUMP_STRENGTH;
        }
        return true;
    }

    public void pause() {
        isPlaying = false;
        try {
            if (gameThread != null) gameThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void resume() {
        isPlaying = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    private class Pipe {
        float x;
        float gapY;
        boolean scored = false;

        Pipe(float startX, float gapY) {
            this.x = startX;
            this.gapY = gapY;
        }

        RectF getTopRect() {
            return new RectF(x, 0, x + PIPE_WIDTH, gapY - PIPE_GAP / 2);
        }

        RectF getBottomRect() {
            return new RectF(x, gapY + PIPE_GAP / 2, x + PIPE_WIDTH, screenHeight - 50);
        }
    }
}
