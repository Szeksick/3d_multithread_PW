package mainstage;
/**
 * Symulacja obiektow 3d
 *
 * Spełnione wymagania podane przez ulubioną Panią Dr :) :
 * -Na scenie znajduja sie obiekty 3D reprezenrtujace budynki
 * -Oswietlenie animowane - sumuluje wschod i zachod slonca
 * -Do generowania budynkow o losowej wielkosci i polozeniu zostaly uzyte Runnable i thread
 *
 * Przygotowali:
 * Konrad Gugała
 * Seweryn Kołacz
 */

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;


public class Main extends Application {

    private static final float WIDTH = 1000;
    private static final float HEIGHT = 800;

    private double anchorX, anchorY;
    private double anchorAngleX = 0;
    private double anchorAngleY = 0;
    private final DoubleProperty angleX = new SimpleDoubleProperty(0);
    private final DoubleProperty angleY = new SimpleDoubleProperty(0);

    @Override
    public void start(Stage primaryStage) {
        //glowna metoda rozpoczynana przy starcie aplikacji na glownym, watku javafx odpowiadajacym za wyswietlanie i aktualizacje sceny
        //inicjalizacja podloza w naszej aplikacji
        Box ground = prepareBox(10000,20,10000, "/assets/city.jpg");
        // glowna grupa elementow wyswietlana w scenie
        Group group = new Group();
        //dodanie elementow do grupy
        group.getChildren().add(ground);
        group.getChildren().addAll(prepareLightSource());
        //kamera
        Camera camera = new PerspectiveCamera(true);
        camera.setNearClip(1);
        camera.setFarClip(5000);
        camera.translateZProperty().set(-1000);
        //scena
        Scene scene = new Scene(group, WIDTH, HEIGHT, true);
        scene.setFill(Color.SILVER);
        scene.setCamera(camera);
        //przesuniecie boxa ktory jest uzyty jako podloze tak aby jego środek był w punkcie X0, Y0, Z(wysokośc boxa)
        ground.translateXProperty().set(0);
        ground.translateZProperty().set(0);
        ground.translateYProperty().set(20);

        //jezeli kliknieta spacja to uruchomione zostaje zadanie na watku w tle odpowiadajace za dodanie budynkow
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.SPACE) {

                startTask(group);
            }
        });
        //inicjalizacja sterowania myszą
        initMouseControl(group, scene, primaryStage);

        primaryStage.setTitle("PSK - PW - projekt");
        primaryStage.setScene(scene);
        primaryStage.show();

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                light.setRotate(light.getRotate() + 1);
            }
        };
        timer.start();
    }
    private void startTask(Group group) {
        // Tworzenie Runnable
        Runnable task = () -> runTask(group);

        // Rozpocznij zadanie na watku w tle
        Thread backgroundThread = new Thread(task);
        // Jezeli aplikacja zostanie zamknieta to konczy watek
        backgroundThread.setDaemon(true);
        // Start wątku
        backgroundThread.start();
    }

    private void runTask(Group group) {
        for(int i = 0;i<10;i++){
            try {
                Platform.runLater(() -> {
                    /*Platform.runLater pozwala na bezpieczne uruchomienie watku w tle
                        Do manipulowania sceną dostęp jest tylko przez glowny watek wiec zamieszcenie ponizszego kodu poza parametrem
                        tej funkcji spowodowalo by pojawienie sie wyjkatku w glownym watku
                     */
                    Box box = prepareBox((int)(Math.random() * 100) + 20, (int)(Math.random() * 500)+20,(int)(Math.random() * 100)+20, "/assets/glass.jpg");
                    group.getChildren().add(box);
                    box.translateXProperty().set((int)(Math.random() * 1500) - 500);
                    box.translateZProperty().set((int)(Math.random() * 1500) - 300);
                    box.translateYProperty().set(0-(box.getHeight()/2));
                });

            Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private final PointLight light = new PointLight();

    private Node[] prepareLightSource() {
        //nowy node dla sceny - dodanie swiatla schowanego w sferze
        light.setColor(Color.LIGHTYELLOW);
        light.getTransforms().add(new Translate(100, -1500, 100));
        light.setRotationAxis(Rotate.Z_AXIS);
        //kula grajaca slonce ;)
        Sphere sun = new Sphere(100);
        sun.getTransforms().setAll(light.getTransforms());
        sun.rotateProperty().bind(light.rotateProperty());
        sun.rotationAxisProperty().bind(light.rotationAxisProperty());

        return new Node[]{light, sun};
    }

    private Box prepareBox(int width, int height, int depth, String texture) {
        //metoda tworzaca budynek
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseMap(new Image(getClass().getResourceAsStream(texture)));

        Box box = new Box(width, height, depth);
        box.setMaterial(material);
        return box;
    }

    private void initMouseControl(Group group, Scene scene, Stage stage) {
    // obsluga myszy
        Rotate xRotate;
        Rotate yRotate;
        group.getTransforms().addAll(
                xRotate = new Rotate(0, Rotate.X_AXIS),
                yRotate = new Rotate(0, Rotate.Y_AXIS)
        );
        xRotate.angleProperty().bind(angleX);
        yRotate.angleProperty().bind(angleY);
        //zlapanie mysza ekranu aby za pomoca funkcji nizej obracac grupe wzgledem kamery
        scene.setOnMousePressed(event -> {
            anchorX = event.getSceneX();
            anchorY = event.getSceneY();
            anchorAngleX = angleX.get();
            anchorAngleY = angleY.get();
        });
        //obracanie grupy wzgledem kamery
        scene.setOnMouseDragged(event -> {
            angleX.set(anchorAngleX - (anchorY - event.getSceneY()));
            angleY.set(anchorAngleY + anchorX - event.getSceneX());
        });

        //oddalanie/przyblizanie grupy na scenie od/do kamery na scroll
        stage.addEventHandler(ScrollEvent.SCROLL, event -> {
            double delta = event.getDeltaY();
            group.translateZProperty().set(group.getTranslateZ() + delta);
        });
    }

    public static void main(String[] args) {
        launch(args);
    }

}
