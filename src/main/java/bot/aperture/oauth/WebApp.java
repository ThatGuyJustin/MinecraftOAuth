package bot.aperture.oauth;

import bot.aperture.BungeeMain;
import bot.aperture.SpigotMain;
import bot.aperture.utils.LoggerBungee;
import bot.aperture.utils.LoggerSpigot;

import io.javalin.Javalin;
import io.javalin.core.util.JavalinLogger;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;

import io.mikael.urlbuilder.UrlBuilder;

import okhttp3.*;
import org.eclipse.jetty.util.ajax.JSON;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class WebApp {
    protected static final OkHttpClient httpClient = new OkHttpClient();

    private final Javalin  app;

    private String HOST;
    private int PORT;

    private BungeeMain Bungee;
    private SpigotMain Spigot;

    private static String client_id;
    private static String client_secret;
    private static String redirect_uri;
    private static String scope;

    private UrlBuilder AuthURL;

    public WebApp(BungeeMain Bungee) {
        HOST = Bungee.getConfig().getString("web.host");
        PORT = Bungee.getConfig().getInt("web.port");

        client_id = Bungee.getConfig().getString("discord.client_id");
        client_secret =  Bungee.getConfig().getString("discord.client_secret");
        redirect_uri  = Bungee.getConfig().getString("discord.redirect_uri");

        if (Bungee.getConfig().getBoolean("discord.check_servers")) {
            scope = "identify guilds";
        } else {
            scope = "identify";
        }

        AuthURL = UrlBuilder.fromString("https://discord.com/api/oauth2/authorize")
                .addParameter("prompt", "consent")
                .addParameter("client_id", Bungee.getConfig().getString("discord.client_id"))
                .addParameter("scope", scope)
                .addParameter("redirect_uri", Bungee.getConfig().getString("discord.redirect_uri"))
                .addParameter("response_type", "code");

        JavalinLogger.enabled = false;
        app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.addStaticFiles("/web", Location.CLASSPATH);
            config.requestLogger((ctx, timeMs) -> LoggerBungee.info(String.format("[Webserver] %s %s %s took %s ms ", ctx.status(), ctx.method(), ctx.path(), timeMs), true));
        });

        LoggerBungee.debug(AuthURL.toString());

        app.get("/auth", ctx -> ctx.redirect(AuthURL.toString(), 307));
        app.get("/callback", WebApp::OauthCallback);


        app.error(403, WebApp::ErrorPageRender);
        app.error(404, WebApp::ErrorPageRender);
        app.error(500, WebApp::ErrorPageRender);
        app.error(900, WebApp::ErrorPageRender);
    }

    public WebApp(SpigotMain Spigot) {
        HOST = Spigot.getConfig().getString("web.host");
        PORT = Spigot.getConfig().getInt("web.port");

        String scope;

        this.client_id = Spigot.getConfig().getString("discord.client_id");
        this.client_secret = Spigot.getConfig().getString("discord.client_secret");
        this.redirect_uri  = Spigot.getConfig().getString("discord.redirect_uri");

        if (Spigot.getConfig().getBoolean("discord.check_servers")) {
            scope = "identify guilds";
        } else {
            scope = "identify";
        }

        AuthURL = UrlBuilder.fromString("https://discord.com/api/oauth2/authorize")
                .addParameter("prompt", "consent")
                .addParameter("client_id", Spigot.getConfig().getString("discord.client_id"))
                .addParameter("scope", scope)
                .addParameter("redirect_uri", Spigot.getConfig().getString("discord.redirect_uri"));

        JavalinLogger.enabled = false;
        app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.addStaticFiles("/web", Location.CLASSPATH);
            config.requestLogger((ctx, timeMs) -> LoggerSpigot.info(String.format("[Webserver] %s %s %s took %s ms ", ctx.status(), ctx.method(), ctx.path(), timeMs), true));
        });



        app.get("/auth", ctx -> ctx.redirect(AuthURL.toString(), 307));
        app.get("/callback", WebApp::OauthCallback);

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
        if(this.Spigot != null) LoggerSpigot.log(String.format("[Webserver] " + "Started, Listening on: http://%s:%s", HOST, PORT), true);
        else LoggerBungee.log(String.format("[Webserver] " + "Started, Listening on: http://%s:%s", HOST, PORT), true);
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
    }


    private static void OauthCallback(@NotNull Context ctx) {
        //Do something here to take the code, given as a pram and yeet yeet it to get a barrer/refresh token
        String code = ctx.queryParam("code");

        FormBody body = new FormBody.Builder().add("client_id", client_id)
                .add("client_secret", client_secret)
                .add("redirect_uri", redirect_uri)
                .add("grant_type", "authorization_code")
                .add("scopes", scope)
                .add("code", code)
                .build();

        Request req = new Request.Builder().url("https://discord.com/api/oauth2/token").post(body).build();

        try (Response resp = httpClient.newCall(req).execute()){
            ResponseBody resBody = resp.body();

            String bodyStr = resBody.string();
            if(resp.code() != 200) {
                System.out.println(bodyStr);
            }

            JSONObject bodyJson = new JSONObject(new JSONTokener(bodyStr));
            LoggerBungee.debug(bodyJson.getString("access_token"));

        } catch (IOException e) {
            e.printStackTrace();
        }

        ctx.result("Discord and mc linked");
    }

}