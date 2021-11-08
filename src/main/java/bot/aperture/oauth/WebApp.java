package bot.aperture.oauth;

import bot.aperture.BungeeMain;
import bot.aperture.SpigotMain;
import bot.aperture.utils.LoggerBungee;
import bot.aperture.utils.LoggerSpigot;

import io.javalin.Javalin;
import io.javalin.core.util.JavalinLogger;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class WebApp {
    private final Javalin  app;

    private String HOST;
    private int PORT;

    private BungeeMain Bungee;
    private SpigotMain Spigot;

    public WebApp(BungeeMain Bungee) {
        HOST = Bungee.getConfig().getString("web.host");
        PORT = Bungee.getConfig().getInt("web.port");

        JavalinLogger.enabled = true;
        app = Javalin.create(config -> {
            config.showJavalinBanner = true;
            config.addSinglePageRoot("/", "/index.html", Location.CLASSPATH);
            config.requestLogger((ctx, timeMs) -> LoggerBungee.info(String.format("[Webserver] %s %s %s took %s ms ", ctx.status(), ctx.method(), ctx.path(), timeMs), true));
        });

        app.error(403, WebApp::ErrorPageRender);
        app.error(404, WebApp::ErrorPageRender);
        app.error(500, WebApp::ErrorPageRender);
        app.error(900, WebApp::ErrorPageRender);
    }

    public WebApp(SpigotMain Spigot) {
        HOST = Spigot.getConfig().getString("web.host");
        PORT = Spigot.getConfig().getInt("web.port");


        JavalinLogger.enabled = false;
        app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.addStaticFiles("/web", Location.CLASSPATH);
            config.requestLogger((ctx, timeMs) -> LoggerSpigot.info(String.format("[Webserver] %s %s %s took %s ms ", ctx.status(), ctx.method(), ctx.path(), timeMs), true));
        });

        app.error(403, WebApp::ErrorPageRender);
        app.error(404, WebApp::ErrorPageRender);
        app.error(500, WebApp::ErrorPageRender);
        app.error(900, WebApp::ErrorPageRender);
    }

    public void start() {
        // Yeet the class loader to the plugins class loader, then bring it back because we're feeling kinda weird today
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        if(this.Spigot != null) Thread.currentThread().setContextClassLoader(SpigotMain.class.getClassLoader());
        else Thread.currentThread().setContextClassLoader(BungeeMain.class.getClassLoader());
        app.start(HOST, PORT);
        Thread.currentThread().setContextClassLoader(cl);
        LoggerSpigot.log(String.format("[Webserver] " + "Started, Listening on: http://%s:%s", HOST, PORT), true);
    }

    public void stop() {
        if (app != null) {
            app.stop();
            LoggerSpigot.log("[Webserver] Stopped", true);
        }
    }


    private static void ErrorPageRender(@NotNull Context ctx) {
        String ErrorStr;
        Map<String, Object> page = new HashMap<>();
        page.put("ErrorCode", ctx.status());
        switch (ctx.status()) {
            case 403:
                ErrorStr = "Permission denied";
                break;
            case 404:
                ErrorStr = "File not found :)";
                break;
            default:
                ErrorStr = "Internal Server error";
                break;
        }
        ctx.result(String.format("%s %s", ctx.status(), ErrorStr));

        ctx.render("web/templates/error.pebble", page);
    }
}