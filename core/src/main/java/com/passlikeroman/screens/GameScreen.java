package com.passlikeroman.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.passlikeroman.PassLikeRoman;

public class GameScreen extends ScreenAdapter {

    // ============================================================
    // MUNDO
    // ============================================================

    private static final float WORLD_WIDTH = 900f;
    private static final float WORLD_HEIGHT = 520f;

    // ============================================================
    // SLOTS FIJOS DE PANTALLA
    //
    // Esta es la parte que soluciona el bug de la plataforma
    // cortada. El pasador SIEMPRE está en SENDER_X.
    // El receptor SIEMPRE está en RECEIVER_X.
    // Nunca se calculan posiciones intermedias "aproximadas".
    // ============================================================

    private static final float SENDER_X = 150f;
    private static final float RECEIVER_X = 560f;
    private static final float SCROLL_DISTANCE = RECEIVER_X - SENDER_X;

    // Rango de alturas donde puede aparecer una plataforma.
    // Elegido para que SIEMPRE se vea completa en pantalla,
    // sin pisar el HUD de arriba ni las tribunas de abajo.
    private static final float MIN_PLATFORM_Y = 95f;
    private static final float MAX_PLATFORM_Y = 300f;

    // ============================================================
    // LIBGDX
    // ============================================================

    private final PassLikeRoman game;
    private final OrthographicCamera camera;
    private final ShapeRenderer shapes;
    private final SpriteBatch batch;
    private final BitmapFont font;

    // ============================================================
    // JUGADORES Y PLATAFORMAS — SIEMPRE 2, NUNCA 3
    // ============================================================

    private final Player[] players = new Player[2];
    private final Platform[] platforms = new Platform[2];

    private static final int SENDER = 0;
    private static final int RECEIVER = 1;

    private final Ball ball = new Ball();
    private Obstacle obstacle;

    // ============================================================
    // ESTADO
    // ============================================================

    private int score = 0;
    private int coins = 0;
    private int round = 1;

    private boolean dragging = false;
    private boolean gameOver = false;
    private boolean scrolling = false;

    // ============================================================
    // SCROLL
    // ============================================================

    private float scrollTimer = 0f;
    private final float scrollDuration = 0.5f;
    private float scrollAmount = 0f;

    // ============================================================
    // CLIMA
    // ============================================================

    private enum WeatherEffect {
        NONE, FOG, WIND, STORM, RAIN
    }

    private WeatherEffect currentWeather = WeatherEffect.NONE;

    private float lightningTimer = 0f;

    // Viento: positivo empuja la pelota a la derecha,
    // negativo la empuja a la izquierda.
    private float wind = 0f;

    // Tormenta: mueve la plataforma del receptor.
    private float stormMovement = 0f;
    private float stormDirection = 1f;

    // Lluvia: gravedad extra acumulada mientras la pelota vuela.
    private static final float RAIN_EXTRA_GRAVITY = 260f;
    private float rainFlightTime = 0f;

    // ============================================================
    // INPUT
    // ============================================================

    private final Vector2 mouse = new Vector2();
    private final Vector2 dragPoint = new Vector2();
    private final Vector2 launchVelocity = new Vector2();

    // ============================================================
    // CONSTRUCTOR
    // ============================================================

    public GameScreen(PassLikeRoman game) {

        this.game = game;

        camera = new OrthographicCamera();
        shapes = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();

        platforms[SENDER] = new Platform(SENDER_X, 180, 190, 24);
        platforms[RECEIVER] = new Platform(RECEIVER_X, 230, 190, 24);

        players[SENDER] = new Player("JUGADOR", 10, 0, 0, 35, 92, 82, 90);
        players[RECEIVER] = createRandomPlayer();

        resetPositions();

        Gdx.input.setInputProcessor(new InputAdapter() {

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {

                if (button != Input.Buttons.LEFT || scrolling) return false;

                screenToWorld(screenX, screenY, mouse);

                if (gameOver) {
                    restart();
                    return true;
                }

                if (!ball.flying && mouse.dst(ball.position) < 42) {
                    dragging = true;
                    dragPoint.set(mouse);
                }

                return true;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {

                screenToWorld(screenX, screenY, mouse);

                if (dragging) dragPoint.set(mouse);

                return true;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {

                if (button != Input.Buttons.LEFT) return false;

                screenToWorld(screenX, screenY, mouse);

                if (dragging) {
                    dragPoint.set(mouse);
                    launch();
                    dragging = false;
                }

                return true;
            }

            @Override
            public boolean keyDown(int keycode) {

                if (keycode == Input.Keys.R && gameOver) restart();

                if (keycode == Input.Keys.M) {
                    game.setScreen(new MenuScreen(game));
                }

                return true;
            }
        });
    }

    // ============================================================
    // POSICIONES INICIALES
    // ============================================================

    private void resetPositions() {

        platforms[SENDER].x = SENDER_X;
        platforms[RECEIVER].x = RECEIVER_X;

        updatePlayerPositions();

        ball.reset(
            players[SENDER].getFootX(),
            players[SENDER].getFootY()
        );

        rainFlightTime = 0f;

        chooseRandomWeather();
        createObstacle();
    }

    private void updatePlayerPositions() {

        for (int i = 0; i < 2; i++) {
            players[i].x = platforms[i].getPlayerX();
            players[i].y = platforms[i].getPlayerY();
        }
    }

    private Player createRandomPlayer() {

        int potencia = MathUtils.random(20, 45);
        int recepcion = MathUtils.random(75, 95);
        int precision = MathUtils.random(75, 95);
        int efecto = MathUtils.random(70, 95);

        return new Player("JUGADOR", 8, 0, 0, potencia, recepcion, precision, efecto);
    }

    // ============================================================
    // REINICIAR
    // ============================================================

    private void restart() {

        score = 0;
        coins = 0;
        round = 1;

        gameOver = false;
        scrolling = false;
        scrollTimer = 0;
        scrollAmount = 0;

        wind = 0;
        stormMovement = 0;
        lightningTimer = 0;

        platforms[SENDER] = new Platform(SENDER_X, 180, 190, 24);
        platforms[RECEIVER] = new Platform(RECEIVER_X, 230, 190, 24);

        players[SENDER] = new Player("JUGADOR", 10, 0, 0, 35, 92, 82, 90);
        players[RECEIVER] = createRandomPlayer();

        resetPositions();
    }

    // ============================================================
    // CLIMA ALEATORIO
    // ============================================================

    private void chooseRandomWeather() {

        int random = MathUtils.random(0, 4);

        wind = 0;
        stormMovement = 0;
        stormDirection = 1;

        switch (random) {

            case 0:
                currentWeather = WeatherEffect.NONE;
                break;

            case 1:
                currentWeather = WeatherEffect.FOG;
                break;

            case 2:
                currentWeather = WeatherEffect.WIND;

                // Viento chico: empuja, no reemplaza la puntería.
                // Positivo = derecha, negativo = izquierda.
                wind = MathUtils.random(-24f, 24f);

                // Evitamos que salga "viento" casi nulo,
                // porque ahí no se notaría ni se podría avisar
                // la dirección con claridad.
                if (Math.abs(wind) < 8f) {
                    wind = wind < 0 ? -8f : 8f;
                }

                break;

            case 3:
                currentWeather = WeatherEffect.STORM;
                break;

            case 4:
                currentWeather = WeatherEffect.RAIN;
                break;
        }
    }

    // ============================================================
    // LANZAMIENTO
    // ============================================================

    private void launch() {

        if (ball.flying || scrolling || gameOver) return;

        float dx = ball.position.x - dragPoint.x;
        float dy = ball.position.y - dragPoint.y;

        float distance = MathUtils.clamp(
            (float) Math.sqrt(dx * dx + dy * dy), 0, 150
        );

        if (distance < 18) return;

        float power = players[SENDER].potencia / 100f;
        float speed = (6.2f + power * 3.0f) * distance;

        launchVelocity.set(dx, dy).nor().scl(speed);
        launchVelocity.y += players[SENDER].efecto * 1.15f;

        rainFlightTime = 0f;

        ball.launch(launchVelocity);
    }

    // ============================================================
    // UPDATE
    // ============================================================

    private void update(float delta) {

        if (gameOver) return;

        if (scrolling) {
            updateScroll(delta);
            return;
        }

        if (obstacle != null) obstacle.update(delta);

        if (currentWeather == WeatherEffect.STORM) {
            updateStorm(delta);
        }

        lightningTimer += delta;

        if (ball.flying) {

            float currentWind =
                currentWeather == WeatherEffect.WIND ? wind : 0;

            ball.update(delta, currentWind, players[SENDER].efecto * 1.8f);

            // ----------------------------------------------------
            // LLUVIA: gravedad extra aplicada manualmente.
            // ----------------------------------------------------

            if (currentWeather == WeatherEffect.RAIN) {

                float previousDrop =
                    0.5f * RAIN_EXTRA_GRAVITY * rainFlightTime * rainFlightTime;

                rainFlightTime += delta;

                float newDrop =
                    0.5f * RAIN_EXTRA_GRAVITY * rainFlightTime * rainFlightTime;

                ball.position.y -= (newDrop - previousDrop);
            }

            checkBallBounds();

            if (ball.flying && checkReceiver()) {
                successfulPass();
                return;
            }

            if (ball.flying && checkObstacle()) {
                lose();
            }
        }
    }

    private void updateStorm(float delta) {

        stormMovement += stormDirection * 45f * delta;

        if (stormMovement > 40) {
            stormMovement = 40;
            stormDirection = -1;
        }

        if (stormMovement < -40) {
            stormMovement = -40;
            stormDirection = 1;
        }

        platforms[RECEIVER].x = RECEIVER_X + stormMovement;

        players[RECEIVER].x = platforms[RECEIVER].getPlayerX();
        players[RECEIVER].y = platforms[RECEIVER].getPlayerY();
    }

    private void checkBallBounds() {

        if (
            ball.position.x < -80 ||
                ball.position.x > WORLD_WIDTH + 80 ||
                ball.position.y < 55 ||
                ball.position.y > WORLD_HEIGHT + 80
        ) {
            lose();
        }
    }

    private boolean checkReceiver() {

        Player receiver = players[RECEIVER];

        float distance = ball.position.dst(receiver.x, receiver.y + 10);
        float radius = receiver.getReceptionRadius();

        return distance <= radius;
    }

    private boolean checkObstacle() {

        if (obstacle == null || !obstacle.active) return false;

        float left = obstacle.x - obstacle.width / 2f;
        float right = obstacle.x + obstacle.width / 2f;
        float bottom = obstacle.y;
        float top = obstacle.y + obstacle.height;

        return
            ball.position.x > left &&
                ball.position.x < right &&
                ball.position.y > bottom &&
                ball.position.y < top;
    }

    // ============================================================
    // PASE EXITOSO
    // ============================================================

    private void successfulPass() {

        ball.flying = false;

        score += 100;
        coins += 15;

        if (obstacle != null && obstacle.active) {
            coins += obstacle.reward();
        }

        round++;

        obstacle = null;
        startScroll();
    }

    private void startScroll() {

        scrolling = true;
        scrollTimer = 0;
        scrollAmount = 0;

        stormMovement = 0;
        stormDirection = 1;
    }

    // ============================================================
    // ANIMACIÓN DEL SCROLL
    //
    // Siempre se mueve EXACTAMENTE SCROLL_DISTANCE.
    // Eso es lo que garantiza que la plataforma del receptor
    // caiga justo en SENDER_X, ni un pixel de más ni de menos.
    // ============================================================

    private void updateScroll(float delta) {

        scrollTimer += delta;

        float progress = MathUtils.clamp(scrollTimer / scrollDuration, 0, 1);

        float target = MathUtils.lerp(0, -SCROLL_DISTANCE, progress);
        float step = target - scrollAmount;
        scrollAmount += step;

        platforms[SENDER].move(step);
        platforms[RECEIVER].move(step);

        updatePlayerPositions();

        if (progress >= 1) {
            finishScroll();
        }
    }

    private void finishScroll() {

        scrolling = false;
        scrollAmount = 0;

        // --------------------------------------------------------
        // La plataforma del receptor (que terminó en SENDER_X)
        // pasa a ser la del nuevo pasador.
        // --------------------------------------------------------

        Platform newSenderPlatform = platforms[RECEIVER];
        newSenderPlatform.x = SENDER_X; // corregimos cualquier arrastre de float

        // --------------------------------------------------------
        // La plataforma vieja del pasador se recicla y reaparece
        // a la derecha con una altura nueva.
        // --------------------------------------------------------

        Platform newReceiverPlatform = platforms[SENDER];
        newReceiverPlatform.x = RECEIVER_X;
        newReceiverPlatform.y = MathUtils.random(MIN_PLATFORM_Y, MAX_PLATFORM_Y);

        platforms[SENDER] = newSenderPlatform;
        platforms[RECEIVER] = newReceiverPlatform;

        // --------------------------------------------------------
        // El receptor anterior ahora es el pasador.
        // Se crea un jugador nuevo para recibir.
        // --------------------------------------------------------

        Player newSender = players[RECEIVER];
        Player newReceiver = createRandomPlayer();

        players[SENDER] = newSender;
        players[RECEIVER] = newReceiver;

        updatePlayerPositions();

        ball.reset(players[SENDER].getFootX(), players[SENDER].getFootY());
        rainFlightTime = 0f;

        chooseRandomWeather();
        createObstacle();
    }

    // ============================================================
    // OBSTÁCULO — POSICIONADO SOBRE LA LÍNEA REAL DE PASE
    //
    // En vez de coordenadas fijas, interpolamos entre la posición
    // del pasador y del receptor. Así el obstáculo siempre está
    // realmente en el medio del camino, y si no lo esquivás bien,
    // te la corta.
    // ============================================================

    private void createObstacle() {

        float senderChestX = players[SENDER].x;
        float senderChestY = players[SENDER].y + 25;

        float receiverChestX = players[RECEIVER].x;
        float receiverChestY = players[RECEIVER].y + 25;

        // Punto a lo largo del camino (entre 35% y 65%).
        float t = MathUtils.random(0.35f, 0.65f);

        float baseX = MathUtils.lerp(senderChestX, receiverChestX, t);
        float baseY = MathUtils.lerp(senderChestY, receiverChestY, t);

        // Le restamos un poco para que el CENTRO del obstáculo
        // quede sobre esa línea (no la base).
        float obstacleY = MathUtils.clamp(baseY - 35f, 70f, WORLD_HEIGHT - 120f);

        int choice = MathUtils.random(0, 5);

        Obstacle.Type type;

        switch (choice) {
            case 0: type = Obstacle.Type.GOALKEEPER; break;
            case 1: type = Obstacle.Type.INTERCEPTOR; break;
            case 2: type = Obstacle.Type.CONE_WALL; break;
            case 3: type = Obstacle.Type.TIRES; break;
            case 4: type = Obstacle.Type.WIND_GUST; break;
            default: type = Obstacle.Type.CARDBOARD; break;
        }

        obstacle = new Obstacle(type, baseX, obstacleY);
    }

    private void lose() {
        ball.flying = false;
        gameOver = true;
    }

    private void screenToWorld(int screenX, int screenY, Vector2 result) {

        result.x = screenX * WORLD_WIDTH / Gdx.graphics.getWidth();
        result.y = WORLD_HEIGHT - screenY * WORLD_HEIGHT / Gdx.graphics.getHeight();
    }

    // ============================================================
    // RENDER
    // ============================================================

    @Override
    public void render(float delta) {

        update(delta);

        Gdx.gl.glClearColor(0.025f, 0.05f, 0.09f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.setToOrtho(false, WORLD_WIDTH, WORLD_HEIGHT);
        camera.update();

        shapes.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);

        drawStadium();
        drawPlatforms();

        if (!scrolling) drawObstacle();

        drawPlayers();
        drawBall();

        if (dragging && !ball.flying && currentWeather != WeatherEffect.FOG) {
            drawTrajectory();
        }

        drawHud();



        if (gameOver) drawGameOver();
    }

    private void drawStadium() {

        shapes.begin(ShapeRenderer.ShapeType.Filled);

        shapes.setColor(0.035f, 0.09f, 0.16f, 1);
        shapes.rect(0, 0, WORLD_WIDTH, WORLD_HEIGHT);

        shapes.setColor(0.13f, 0.16f, 0.22f, 1);
        shapes.rect(0, 70, 900, 230);

        shapes.setColor(1f, 0.92f, 0.62f, 0.95f);
        shapes.rect(55, 340, 125, 25);
        shapes.rect(720, 340, 125, 25);

        shapes.setColor(0.05f, 0.32f, 0.12f, 1);
        shapes.rect(0, 70, 900, 150);

        for (int x = 0; x < 900; x += 120) {
            shapes.setColor(0.07f, 0.38f, 0.15f, 1);
            shapes.rect(x, 70, 60, 150);
        }

        shapes.setColor(0.9f, 0.95f, 0.9f, 0.55f);
        shapes.rect(0, 90, 900, 3);
        shapes.rect(0, 217, 900, 3);
        shapes.rect(450, 90, 3, 130);
        shapes.circle(450, 155, 42);

        shapes.end();

        // Lluvia visual: dibujamos siempre que el clima sea RAIN.
        if (currentWeather == WeatherEffect.RAIN) {

            shapes.begin(ShapeRenderer.ShapeType.Line);
            shapes.setColor(0.55f, 0.72f, 0.9f, 0.5f);

            for (int i = 0; i < 130; i++) {
                float x = (i * 83 + round * 17) % 900;
                float y = 70 + (i * 47) % 430;

                shapes.line(x, y, x - 8, y - 24);
            }

            shapes.end();
        }
    }

    private void drawPlatforms() {

        shapes.begin(ShapeRenderer.ShapeType.Filled);

        for (int i = 0; i < platforms.length; i++) {

            Platform p = platforms[i];

            if (p.x < -200 || p.x > 1000) continue;

            shapes.setColor(0.22f, 0.13f, 0.07f, 1);
            shapes.triangle(
                p.x + 20, p.y,
                p.x + p.width - 20, p.y,
                p.x + p.width / 2f, p.y - 65
            );

            shapes.setColor(
                i == SENDER ? 0.08f : 0.12f,
                i == SENDER ? 0.48f : 0.55f,
                i == SENDER ? 0.16f : 0.20f,
                1
            );

            shapes.rect(p.x, p.y, p.width, p.height);

            shapes.setColor(0.8f, 0.9f, 0.75f, 0.9f);
            shapes.rect(p.x + 10, p.y + p.height - 4, p.width - 20, 4);

            shapes.setColor(0.3f, 0.2f, 0.12f, 1);
            shapes.rect(p.x + 15, p.y + p.height, 6, 42);
            shapes.rect(p.x + p.width - 21, p.y + p.height, 6, 42);

            shapes.setColor(0.35f, 0.25f, 0.15f, 1);
            shapes.rect(p.x + 20, p.y + p.height + 28, p.width - 40, 4);
        }

        shapes.end();
    }

    private void drawPlayers() {

        shapes.begin(ShapeRenderer.ShapeType.Filled);

        for (int i = 0; i < 2; i++) {

            Player p = players[i];

            shapes.setColor(0.82f, 0.61f, 0.45f, 1);
            shapes.circle(p.x, p.y + 42, 11);

            shapes.setColor(
                i == SENDER ? 0.05f : 0.04f,
                i == SENDER ? 0.20f : 0.12f,
                i == SENDER ? 0.55f : 0.35f,
                1
            );

            shapes.rect(p.x - 18, p.y + 5, 36, 35);

            shapes.setColor(0.9f, 0.75f, 0.08f, 1);
            shapes.rect(p.x - 18, p.y + 18, 36, 7);

            shapes.setColor(0.92f, 0.92f, 0.92f, 1);
            shapes.rect(p.x - 13, p.y - 15, 10, 22);
            shapes.rect(p.x + 3, p.y - 15, 10, 22);

            shapes.setColor(0.04f, 0.04f, 0.04f, 1);
            shapes.rect(p.x - 16, p.y - 20, 14, 6);
            shapes.rect(p.x + 4, p.y - 20, 14, 6);
        }

        shapes.end();

        if (!ball.flying && !scrolling && !gameOver) {
            drawReceptionArc(players[RECEIVER]);
        }

        batch.begin();

        font.getData().setScale(0.65f);
        font.setColor(1, 1, 1, 1);

        font.draw(batch, "PASA", players[SENDER].x - 25, players[SENDER].y - 32);
        font.draw(batch, "RECIBE", players[RECEIVER].x - 28, players[RECEIVER].y - 32);

        batch.end();
    }

    private void drawReceptionArc(Player p) {

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(0.1f, 1f, 0.25f, 0.95f);

        float radius = p.getReceptionRadius();
        int segments = 32;
        float startAngle = 15f;
        float endAngle = 165f;

        for (int i = 0; i < segments; i++) {

            float a1 = MathUtils.lerp(startAngle, endAngle, i / (float) segments);
            float a2 = MathUtils.lerp(startAngle, endAngle, (i + 1) / (float) segments);

            float x1 = p.x + MathUtils.cosDeg(a1) * radius;
            float y1 = p.y + 18 + MathUtils.sinDeg(a1) * radius;
            float x2 = p.x + MathUtils.cosDeg(a2) * radius;
            float y2 = p.y + 18 + MathUtils.sinDeg(a2) * radius;

            shapes.line(x1, y1, x2, y2);
        }

        shapes.end();
    }

    private void drawBall() {

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(1, 1, 1, 1);
        shapes.circle(ball.position.x, ball.position.y, 9);
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(0.05f, 0.05f, 0.05f, 1);
        shapes.circle(ball.position.x, ball.position.y, 9);
        shapes.end();
    }

    // ============================================================
    // TRAYECTORIA — solamente una fracción, con radio limitado
    // ============================================================

    private void drawTrajectory() {

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(1f, 1f, 1f, 0.85f);

        float dx = ball.position.x - dragPoint.x;
        float dy = ball.position.y - dragPoint.y;

        float distance = MathUtils.clamp(
            (float) Math.sqrt(dx * dx + dy * dy), 0, 150
        );

        if (distance <= 10) {
            shapes.end();
            return;
        }

        float power = players[SENDER].potencia / 100f;
        float speed = (6.2f + power * 3.0f) * distance;

        Vector2 predictedVelocity = new Vector2(dx, dy).nor().scl(speed);
        predictedVelocity.y += players[SENDER].efecto * 1.15f;

        float previewWind = currentWeather == WeatherEffect.WIND ? wind : 0;

        float visibleRadius = 130f;

        for (int i = 1; i <= 40; i++) {

            float t = i * 0.025f;

            float px = ball.position.x
                + predictedVelocity.x * t
                + 0.5f * previewWind * t * t;

            float py = ball.position.y
                + predictedVelocity.y * t
                - 0.5f * 520f * t * t;

            if (currentWeather == WeatherEffect.RAIN) {
                py -= 0.5f * RAIN_EXTRA_GRAVITY * t * t;
            }

            float distanceFromPlayer = Vector2.dst(
                ball.position.x, ball.position.y, px, py
            );

            if (distanceFromPlayer > visibleRadius) break;

            if (i % 2 == 0) {
                shapes.circle(px, py, 3f);
            }
        }

        shapes.end();
    }

    private void drawObstacle() {

        if (obstacle == null || !obstacle.active) return;

        shapes.begin(ShapeRenderer.ShapeType.Filled);

        switch (obstacle.type) {

            case GOALKEEPER:
                shapes.setColor(0.95f, 0.75f, 0.08f, 1);
                shapes.circle(obstacle.x, obstacle.y + 60, 10);
                shapes.setColor(0.85f, 0.1f, 0.12f, 1);
                shapes.rect(obstacle.x - 16, obstacle.y + 20, 32, 38);
                break;

            case INTERCEPTOR:
                shapes.setColor(0.82f, 0.61f, 0.45f, 1);
                shapes.circle(obstacle.x, obstacle.y + 65, 10);
                shapes.setColor(0.95f, 0.2f, 0.15f, 1);
                shapes.rect(obstacle.x - 15, obstacle.y + 20, 30, 40);
                break;

            case CONE_WALL:
                shapes.setColor(1f, 0.48f, 0.05f, 1);
                for (int i = 0; i < 4; i++) {
                    shapes.triangle(
                        obstacle.x - 32 + i * 22, obstacle.y,
                        obstacle.x - 22 + i * 22, obstacle.y,
                        obstacle.x - 27 + i * 22, obstacle.y + 28
                    );
                }
                break;

            case TIRES:
                shapes.setColor(0.04f, 0.04f, 0.04f, 1);
                for (int i = 0; i < 3; i++) {
                    shapes.circle(obstacle.x - 22 + i * 22, obstacle.y + 18, 18);
                }
                break;

            case WIND_GUST:
                shapes.setColor(0.4f, 0.8f, 1f, 0.35f);
                shapes.rect(obstacle.x - 25, obstacle.y, 50, 90);
                break;

            case CARDBOARD:
                shapes.setColor(0.62f, 0.42f, 0.22f, 1);
                shapes.rect(obstacle.x - 19, obstacle.y, 38, 55);
                break;
        }

        shapes.end();

        batch.begin();

        font.getData().setScale(0.55f);
        font.setColor(1, 1, 1, 1);

        font.draw(
            batch, "+" + obstacle.reward(),
            obstacle.x - 18, obstacle.y + obstacle.height + 15
        );

        batch.end();
    }

    private void drawHud() {

        shapes.begin(ShapeRenderer.ShapeType.Filled);

        shapes.setColor(0.02f, 0.07f, 0.13f, 0.88f);
        shapes.rect(15, 425, 245, 75);
        shapes.rect(300, 450, 380, 48);

        shapes.end();

        batch.begin();

        font.getData().setScale(1.15f);
        font.setColor(1, 1, 1, 1);
        font.draw(batch, "SCORE", 28, 480);
        font.draw(batch, String.valueOf(score), 210, 480);

        font.setColor(1, 0.78f, 0.15f, 1);
        font.draw(batch, "COINS", 28, 450);
        font.draw(batch, String.valueOf(coins), 210, 450);

        font.setColor(1, 1, 1, 1);
        font.getData().setScale(0.65f);
        font.draw(batch, "RONDA " + round, 28, 430);

        String weatherText;

        switch (currentWeather) {

            case FOG:
                weatherText = "NIEBLA";
                break;

            case WIND:
                weatherText = wind > 0
                    ? "VIENTO -> DERECHA"
                    : "VIENTO <- IZQUIERDA";
                break;

            case STORM:
                weatherText = "TORMENTA";
                break;

            case RAIN:
                weatherText = "LLUVIA";
                break;

            default:
                weatherText = "CLIMA NORMAL";
                break;
        }

        font.getData().setScale(0.85f);
        font.draw(batch, weatherText, 320, 480);

        batch.end();
    }


    private void drawGameOver() {

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.01f, 0.02f, 0.04f, 0.88f);
        shapes.rect(190, 150, 520, 230);
        shapes.end();

        batch.begin();

        font.getData().setScale(2.2f);
        font.setColor(1, 0.25f, 0.18f, 1);
        font.draw(batch, "GAME OVER", 335, 330);

        font.getData().setScale(1.0f);
        font.setColor(1, 1, 1, 1);
        font.draw(batch, "Puntaje: " + score, 365, 280);
        font.draw(batch, "Monedas: " + coins, 365, 250);
        font.draw(batch, "Clic o R para volver a jugar", 315, 205);
        font.draw(batch, "M para volver al menú", 350, 180);

        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        camera.setToOrtho(false, WORLD_WIDTH, WORLD_HEIGHT);
    }

    @Override
    public void dispose() {
        shapes.dispose();
        batch.dispose();
        font.dispose();
    }
}
