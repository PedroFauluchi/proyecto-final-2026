package com.passlikeroman.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.passlikeroman.PassLikeRoman;
import com.passlikeroman.data.GameProfile;

public class GameScreen extends ScreenAdapter {

    // ============================================================
    // MUNDO
    // ============================================================

    private static final float WORLD_WIDTH = 900f;
    private static final float WORLD_HEIGHT = 520f;
    private static final String[] STADIUM_BACKGROUND_FILES = {
        "stadiums/bombonera.png",
        "stadiums/monumental.png",
        "stadiums/cilindro.png",
        "stadiums/libertadores.png",
        "stadiums/nuevo-gasometro.png"
    };
    private static final String[] PLAYER_SPRITE_FILES = {
        "players/boca.png",
        "players/river.png",
        "players/racing.png",
        "players/independiente.png",
        "players/san-lorenzo.png",
        "players/rosario-central.png",
        "players/estudiantes.png",
        "players/newells.png",
        "players/argentinos-juniors.png",
        "players/velez.png",
        "players/huracan.png",
        "players/gimnasia.png",
        "players/neutro.png"
    };
    // Hoja adjunta: cada uniforme tiene una pose a mitad de la patada y otra de remate.
    // -1 mantiene el sprite normal para kits que no aparecen en esa hoja.
    private static final int[] KICK_SPRITE_BY_KIT = {4, -1, 8, 9, 7, 4, 6, 3, 1, 2, 0, 5, 10};
    private static final int BOCA_KIT = 0;
    private static final int RIVER_KIT = 1;

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
    private final Texture fogTexture;
    private final Texture stadiumBackground;
    private final Texture[] playerSprites = new Texture[PLAYER_SPRITE_FILES.length];
    private final Texture[][] kickSprites = new Texture[2][11];
    private final Texture[][] bocaRiverKickSprites = new Texture[2][2];
    private final Texture[][] receptionSprites = new Texture[2][PLAYER_SPRITE_FILES.length];

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
    private float animationTime = 0f;
    private int round = 1;

    private boolean dragging = false;
    private boolean gameOver = false;
    private boolean scrolling = false;
    private boolean scrollAfterReceive = false;

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
    // COLISIONES
    // ============================================================

    private static final float BALL_RADIUS = 9f;

    // Ventana breve después de patear durante la cual la pelota
    // no choca contra la plataforma del propio pasador (si no,
    // chocaría contra su propia base apenas sale del pie, porque
    // nace prácticamente pegada a ella).
    private static final float LIFTOFF_GRACE = 0.15f;
    private float liftoffTimer = 0f;

    // ============================================================
    // INPUT
    // ============================================================

    private final Vector2 mouse = new Vector2();
    private final Vector2 dragPoint = new Vector2();
    private final Vector2 launchVelocity = new Vector2();
    private final Vector2 previousBallPosition = new Vector2();
    private final Vector2 receptionBallStart = new Vector2();
    private static final float RECEPTION_BALL_GRAVITY = 980f;
    private float receptionBallDropTimer = 0f;
    private float receptionBallDropDuration = 0f;

    // ============================================================
    // CONSTRUCTOR
    // ============================================================

    public GameScreen(PassLikeRoman game) {

        this.game = game;

        camera = new OrthographicCamera();
        shapes = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();
        fogTexture = new Texture(Gdx.files.internal("sprites/weather/fog.png"));
        int stadiumIndex = MathUtils.clamp(GameProfile.stadium(), 0, STADIUM_BACKGROUND_FILES.length - 1);
        stadiumBackground = new Texture(Gdx.files.internal(STADIUM_BACKGROUND_FILES[stadiumIndex]));
        for (int i = 0; i < PLAYER_SPRITE_FILES.length; i++) {
            playerSprites[i] = new Texture(Gdx.files.internal(PLAYER_SPRITE_FILES[i]));
        }
        for (int i = 0; i < 10; i++) {
            kickSprites[0][i] = new Texture(Gdx.files.internal("players/actions/kick-mid-" + i + ".png"));
            kickSprites[1][i] = new Texture(Gdx.files.internal("players/actions/kick-finish-" + i + ".png"));
        }
        kickSprites[0][10] = new Texture(Gdx.files.internal("players/actions/neutral-kick-mid.png"));
        kickSprites[1][10] = new Texture(Gdx.files.internal("players/actions/neutral-kick-finish.png"));
        bocaRiverKickSprites[0][0] = new Texture(Gdx.files.internal("players/actions/boca-kick-mid.png"));
        bocaRiverKickSprites[1][0] = new Texture(Gdx.files.internal("players/actions/boca-kick-finish.png"));
        bocaRiverKickSprites[0][1] = new Texture(Gdx.files.internal("players/actions/river-kick-mid.png"));
        bocaRiverKickSprites[1][1] = new Texture(Gdx.files.internal("players/actions/river-kick-finish.png"));
        for (int i = 0; i < PLAYER_SPRITE_FILES.length; i++) {
            receptionSprites[0][i] = new Texture(Gdx.files.internal("players/reception/foot-" + i + ".png"));
            receptionSprites[1][i] = new Texture(Gdx.files.internal("players/reception/chest-" + i + ".png"));
        }

        platforms[SENDER] = new Platform(SENDER_X, 180, 190, 24);
        platforms[RECEIVER] = new Platform(RECEIVER_X, 230, 190, 24);

        players[SENDER] = createPlayer(35, 92, 82, 90);
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

                if (players[RECEIVER].isReceiving()) return true;

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
        liftoffTimer = 0f;

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

        return createPlayer(potencia, recepcion, precision, efecto);
    }

    private Player createPlayer(int potencia, int recepcion, int precision, int efecto) {
        float bonus = GameProfile.kitBonus();
        return new Player("JUGADOR", 8, 0, 0,
            Math.min(100, potencia + bonus), Math.min(100, recepcion + bonus),
            Math.min(100, precision + bonus), Math.min(100, efecto + bonus));
    }

    // ============================================================
    // REINICIAR
    // ============================================================

    private void restart() {

        score = 0;
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

        players[SENDER] = createPlayer(35, 92, 82, 90);
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
                float windLimit = Math.min(55f, 24f + round * 1.5f);
                wind = MathUtils.random(-windLimit, windLimit);

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
        float inaccuracy = (100f - players[SENDER].precision) * 0.025f;
        launchVelocity.rotate(MathUtils.random(-inaccuracy, inaccuracy));
        launchVelocity.y += players[SENDER].efecto * 1.15f;
        players[SENDER].startKick();

        rainFlightTime = 0f;
        liftoffTimer = LIFTOFF_GRACE;

        ball.launch(launchVelocity);
    }

    // ============================================================
    // UPDATE
    // ============================================================

    private void update(float delta) {

        animationTime += delta;
        for (Player player : players) if (player != null) player.update(delta);
        updateReceptionBallDrop(delta);
        if (scrollAfterReceive && !players[RECEIVER].isReceiving()) {
            scrollAfterReceive = false;
            startScroll();
        }

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

        if (liftoffTimer > 0) {
            liftoffTimer -= delta;
        }

        if (ball.flying) {

            float currentWind =
                currentWeather == WeatherEffect.WIND ? wind : 0;

            previousBallPosition.set(ball.position);
            ball.update(delta, currentWind, players[SENDER].efecto * 1.8f);

            // ----------------------------------------------------
            // LLUVIA: gravedad extra aplicada manualmente.
            // ----------------------------------------------------

            if (currentWeather == WeatherEffect.RAIN) {

                float rainGravity = RAIN_EXTRA_GRAVITY * (1f + Math.min(round, 8) * 0.06f);
                float previousDrop = 0.5f * rainGravity * rainFlightTime * rainFlightTime;

                rainFlightTime += delta;

                float newDrop = 0.5f * rainGravity * rainFlightTime * rainFlightTime;

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

            if (ball.flying && checkPlatformCollision()) {
                lose();
            }
        }
    }

    private void updateStorm(float delta) {

        stormMovement += stormDirection * Math.min(100f, 45f + round * 2.5f) * delta;

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

        return sweptCircleIntersectsRect(
            ball.position.x, ball.position.y, BALL_RADIUS,
            previousBallPosition.x, previousBallPosition.y,
            left, bottom, right, top
        );
    }

    // ============================================================
    // COLISIÓN CON PLATAFORMAS
    //
    // Las plataformas también son un obstáculo: si el pase sale
    // corto o muy bajo y la pelota pega contra el cajón (no contra
    // el jugador), se pierde. No se revisa durante el breve
    // "liftoff" después de patear, porque la pelota nace casi
    // pegada a la plataforma del pasador.
    // ============================================================

    private boolean checkPlatformCollision() {

        if (liftoffTimer > 0) return false;

        for (Platform p : platforms) {

            // Plataformas fuera de pantalla (ya recicladas
            // durante el scroll) no cuentan.
            if (p.x < -200 || p.x > 1000) continue;

            if (sweptCircleIntersectsRect(
                ball.position.x, ball.position.y, BALL_RADIUS,
                previousBallPosition.x, previousBallPosition.y,
                p.x, p.y, p.x + p.width, p.y + p.height
            )) {
                return true;
            }
        }

        return false;
    }

    // ============================================================
    // CÍRCULO vs RECTÁNGULO
    //
    // Chequeo real de colisión (no solo "¿el centro está adentro?"),
    // así la pelota no puede atravesar una esquina o un borde fino
    // sin que se detecte el golpe.
    // ============================================================

    private boolean circleIntersectsRect(
        float cx, float cy, float radius,
        float left, float bottom, float right, float top
    ) {

        float closestX = MathUtils.clamp(cx, left, right);
        float closestY = MathUtils.clamp(cy, bottom, top);

        float dx = cx - closestX;
        float dy = cy - closestY;

        return (dx * dx + dy * dy) < (radius * radius);
    }

    /** Samples the ball's movement segment so fast passes cannot skip thin hazards. */
    private boolean sweptCircleIntersectsRect(float cx, float cy, float radius,
                                               float fromX, float fromY,
                                               float left, float bottom, float right, float top) {
        float distance = Vector2.dst(fromX, fromY, cx, cy);
        int steps = Math.max(1, MathUtils.ceil(distance / Math.max(2f, radius * 0.5f)));
        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            if (circleIntersectsRect(MathUtils.lerp(fromX, cx, t), MathUtils.lerp(fromY, cy, t),
                radius, left, bottom, right, top)) return true;
        }
        return false;
    }

    // ============================================================
    // PASE EXITOSO
    // ============================================================

    private void successfulPass() {

        ball.flying = false;
        Player receiver = players[RECEIVER];
        float spriteBottom = receiver.y - 18f;
        float relativeHeight = (ball.position.y - spriteBottom) / 82f;
        Player.ReceiveType receiveType = relativeHeight >= 0.50f
            ? Player.ReceiveType.CHEST
            : Player.ReceiveType.FOOT;
        receiver.startReceive(receiveType);
        receptionBallStart.set(ball.position);
        float dropHeight = Math.max(0f, ball.position.y - receiver.getFootY());
        receptionBallDropDuration = (float) Math.sqrt(2f * dropHeight / RECEPTION_BALL_GRAVITY);
        receptionBallDropTimer = receptionBallDropDuration;
        if (receptionBallDropTimer <= 0f) keepBallAtReceiver();

        score += 100;
        GameProfile.addCoins(15);

        if (obstacle != null && obstacle.active) {
            GameProfile.addCoins(obstacle.reward());
        }

        round++;

        obstacle = null;
        scrollAfterReceive = true;
    }

    private void updateReceptionBallDrop(float delta) {
        if (receptionBallDropTimer <= 0f) return;

        Player receiver = players[RECEIVER];
        receptionBallDropTimer = Math.max(0f, receptionBallDropTimer - delta);
        float elapsed = receptionBallDropDuration - receptionBallDropTimer;
        float progress = receptionBallDropDuration <= 0f ? 1f : elapsed / receptionBallDropDuration;
        ball.position.set(
            MathUtils.lerp(receptionBallStart.x, receiver.x - 14f, progress),
            Math.max(receiver.getFootY(), receptionBallStart.y - 0.5f * RECEPTION_BALL_GRAVITY * elapsed * elapsed)
        );
        if (receptionBallDropTimer == 0f) keepBallAtReceiver();
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
        keepBallAtReceiver();

        if (progress >= 1) {
            finishScroll();
        }
    }

    private void keepBallAtReceiver() {
        Player receiver = players[RECEIVER];
        ball.position.set(receiver.x - 14f, receiver.getFootY());
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
        liftoffTimer = 0f;

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
        obstacle.speed += Math.min(100f, round * 2.5f);
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
        int cropWidth = Math.min(stadiumBackground.getWidth(),
            Math.round(stadiumBackground.getHeight() * WORLD_WIDTH / WORLD_HEIGHT));
        int cropX = (stadiumBackground.getWidth() - cropWidth) / 2;
        batch.begin();
        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(stadiumBackground, 0, 0, WORLD_WIDTH, WORLD_HEIGHT,
            cropX, 0, cropWidth, stadiumBackground.getHeight(), false, false);
        batch.end();

        // Lluvia visual: dibujamos siempre que el clima sea RAIN.
        if (currentWeather == WeatherEffect.RAIN) {

            shapes.begin(ShapeRenderer.ShapeType.Line);
            shapes.setColor(0.55f, 0.72f, 0.9f, 0.5f);

            float fallSpeed = 480f;
            float rangeY = 480f;
            float slant = 6f;

            for (int i = 0; i < 130; i++) {
                float seedX = (i * 83) % 900;
                float seedOffset = (i * 197) % (int) rangeY;
                float fallen = (animationTime * fallSpeed + seedOffset) % rangeY;
                float y = 500f - fallen;
                float x = (seedX - fallen * (slant / rangeY) * 40f + 900f) % 900f;
                shapes.line(x, y, x - 8, y - 24);
            }

            shapes.end();
        }
    }

    private void drawCrowd(float standX, float standY, float standWidth, int rows) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        int cols = 10;
        for (int row = 0; row < rows; row++) for (int col = 0; col < cols; col++) {
            float seed = row * 37 + col * 13;
            float bob = MathUtils.sin(animationTime * 2.4f + (seed % 10) * 0.6f) * 2.2f;
            float pick = (seed % 5) / 5f;
            shapes.setColor(0.5f + pick * 0.4f, 0.3f + (1f - pick) * 0.3f, 0.25f + pick * 0.5f, 1);
            shapes.circle(standX + (col + 0.5f) * standWidth / cols,
                standY + row * 8f + bob, 3.4f);
        }
        shapes.end();
    }

    private void drawFlag(float poleX, float poleBaseY, float poleHeight,
                          float flagWidth, float flagHeight, float r, float g, float b, float phase) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.28f, 0.28f, 0.30f, 1);
        shapes.rect(poleX - 2, poleBaseY, 4, poleHeight);
        float flagX = poleX + 2;
        float flagY = poleBaseY + poleHeight - flagHeight;
        int strips = 12;
        float stripWidth = flagWidth / strips;
        shapes.setColor(r, g, b, 1);
        for (int i = 0; i < strips; i++) {
            float t = i / (float) strips;
            float wave = MathUtils.sin(animationTime * 3.2f + phase + t * 5f) * (3f + t * 9f);
            shapes.rect(flagX + i * stripWidth, flagY + wave, stripWidth + 1, flagHeight);
        }
        shapes.end();
        batch.begin();
        font.getData().setScale(0.32f);
        font.setColor(1, 1, 1, 1);
        font.draw(batch, "PASSLIKEROMAN", flagX - 4,
            flagY + flagHeight * 0.6f + MathUtils.sin(animationTime * 3.2f + phase) * 3f);
        batch.end();
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
        if (!ball.flying && !scrolling && !gameOver) {
            drawReceptionArc(players[RECEIVER]);
        }

        int kit = MathUtils.clamp(GameProfile.kit(), 0, playerSprites.length - 1);
        float spriteWidth = 34f;
        float spriteHeight = 82f;

        batch.begin();
        batch.setColor(1f, 1f, 1f, 1f);
        for (int i = 0; i < players.length; i++) {
            Player player = players[i];
            Texture sprite = playerSprites[kit];
            float frameWidth = spriteWidth;
            int actionSprite = KICK_SPRITE_BY_KIT[kit];
            int specialKickKit = kit == BOCA_KIT ? 0 : kit == RIVER_KIT ? 1 : -1;
            boolean showingReceptionPose = player.isReceiving();
            if (showingReceptionPose) {
                int receivePose = player.getReceiveType() == Player.ReceiveType.CHEST ? 1 : 0;
                sprite = receptionSprites[receivePose][kit];
                frameWidth = receivePose == 0 && (kit == 1 || kit == 12) ? 52f : 36f;
            }
            boolean showingKickPose = false;
            if (!showingReceptionPose && player.isKicking() && (actionSprite >= 0 || specialKickKit >= 0)) {
                float progress = player.getKickProgress();
                if (progress >= 0.18f && progress < 0.56f) {
                    sprite = specialKickKit >= 0 ? bocaRiverKickSprites[0][specialKickKit] : kickSprites[0][actionSprite];
                    frameWidth = specialKickKit >= 0 ? 60f : actionSprite == 10 ? 70f : 47f;
                    showingKickPose = true;
                } else if (progress >= 0.56f && progress < 0.92f) {
                    sprite = specialKickKit >= 0 ? bocaRiverKickSprites[1][specialKickKit] : kickSprites[1][actionSprite];
                    frameWidth = specialKickKit >= 0 ? 60f : actionSprite == 10 ? 70f : 47f;
                    showingKickPose = true;
                }
            }
            float bob = !ball.flying && !scrolling
                ? MathUtils.sin(animationTime * 4f + i) * 2f
                : 0f;
            boolean clubFacingReversed = kit == 6 || kit == 7 || kit == 10 || kit == 11;
            boolean mirrored = (i == RECEIVER) ^ clubFacingReversed;
            boolean receptionFrameNeedsFlip = false;
            boolean flipX = showingReceptionPose
                ? receptionFrameNeedsFlip
                : mirrored ^ (showingKickPose && clubFacingReversed);
            float facing = mirrored ? -1f : 1f;
            float kickPulse = player.isKicking()
                ? MathUtils.sin(player.getKickProgress() * MathUtils.PI)
                : 0f;
            float receivePulse = player.isReceiving()
                ? MathUtils.sin(player.getReceiveProgress() * MathUtils.PI)
                : 0f;
            float drawWidth = frameWidth * (1f + receivePulse * 0.06f);
            float drawHeight = spriteHeight * (1f - receivePulse * 0.09f);
            float drawX = player.x + facing * (kickPulse * 8f + receivePulse * 3f) - drawWidth / 2f;
            float drawY = player.y + bob - 18f;

            batch.draw(sprite,
                drawX, drawY,
                drawWidth, drawHeight,
                0, 0, sprite.getWidth(), sprite.getHeight(),
                flipX, false);
        }

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

        if (ball.flying) {
            shapes.begin(ShapeRenderer.ShapeType.Line);
            shapes.setColor(0.05f, 0.05f, 0.05f, 1);
            float angle = animationTime * 640f;
            for (int i = 0; i < 2; i++) {
                float a = angle + i * 90f;
                float x1 = ball.position.x + MathUtils.cosDeg(a) * 7f;
                float y1 = ball.position.y + MathUtils.sinDeg(a) * 7f;
                float x2 = ball.position.x + MathUtils.cosDeg(a + 180f) * 7f;
                float y2 = ball.position.y + MathUtils.sinDeg(a + 180f) * 7f;
                shapes.line(x1, y1, x2, y2);
            }
            shapes.end();
        }

        if (currentWeather == WeatherEffect.FOG) {
            float drift = MathUtils.sin(animationTime * 0.22f) * 28f;
            float drift2 = MathUtils.sin(animationTime * 0.17f + 2f) * 22f;

            batch.begin();
            batch.setColor(0.86f, 0.91f, 0.96f, 0.28f);
            batch.draw(fogTexture, 300f + drift, 250f, 170f, 128f);
            batch.draw(fogTexture, 374f + drift2, 285f, 175f, 132f);
            batch.draw(fogTexture, 350f - drift2, 215f, 155f, 116f);
            batch.setColor(1f, 1f, 1f, 1f);
            batch.end();
        } else if (currentWeather == WeatherEffect.STORM) {
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(0.035f, 0.045f, 0.075f, 0.30f);
            float drift = (animationTime * 25f) % 220f;
            for (int i = 0; i < 5; i++) {
                float x = i * 220f - drift;
                shapes.ellipse(x, 300 + (i % 2) * 20, 190, 46);
            }
            float flash = Math.max(0f, 1f - (lightningTimer % 6f) * 5f);
            if (flash > 0) {
                shapes.setColor(0.75f, 0.84f, 1f, flash * 0.24f);
                shapes.rect(0, 70, WORLD_WIDTH, WORLD_HEIGHT - 70);
            }
            shapes.end();
        }

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
                py -= 0.5f * RAIN_EXTRA_GRAVITY * (1f + Math.min(round, 8) * 0.06f) * t * t;
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
        font.draw(batch, String.valueOf(GameProfile.coins()), 210, 450);

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
        font.getData().setScale(0.55f);
        font.setColor(0.75f, 0.84f, 0.94f, 1);
        font.draw(batch, GameProfile.STADIUMS[GameProfile.stadium()], 520, 435);

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
        font.draw(batch, "Monedas: " + GameProfile.coins(), 365, 250);
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
        fogTexture.dispose();
        stadiumBackground.dispose();
        for (Texture playerSprite : playerSprites) playerSprite.dispose();
        for (Texture[] poses : kickSprites) for (Texture pose : poses) pose.dispose();
        for (Texture[] poses : bocaRiverKickSprites) for (Texture pose : poses) pose.dispose();
        for (Texture[] poses : receptionSprites) for (Texture pose : poses) pose.dispose();
    }
}
