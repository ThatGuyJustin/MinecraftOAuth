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
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
    private final Map<String, UUID> states = new ConcurrentHashMap<>();
    private final Map<String, UUID> codes = new ConcurrentHashMap<>();

    private UrlBuilder AuthURL;

    public WebApp(BungeeMain Bungee) {
        this.Bungee = Bungee;

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

        JavalinLogger.enabled = false;
        app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.enableDevLogging();
            config.addStaticFiles("/web", Location.CLASSPATH);
            config.requestLogger((ctx, timeMs) -> LoggerBungee.info(String.format("[Webserver] %s %s %s took %s ms ", ctx.status(), ctx.method(), ctx.path(), timeMs), true));
        });

        app.get("/auth", this::Oauth);
        app.get("/callback", this::OauthCallback);

        app.error(403, WebApp::ErrorPageRender);
        app.error(404, WebApp::ErrorPageRender);
        app.error(500, WebApp::ErrorPageRender);
        app.error(900, WebApp::ErrorPageRender);
    }

    public WebApp(SpigotMain Spigot) {
        this.Spigot = Spigot;

        HOST = Spigot.getConfig().getString("web.host");
        PORT = Spigot.getConfig().getInt("web.port");

        this.client_id = Spigot.getConfig().getString("discord.client_id");
        this.client_secret = Spigot.getConfig().getString("discord.client_secret");
        this.redirect_uri  = Spigot.getConfig().getString("discord.redirect_uri");

        if (Spigot.getConfig().getBoolean("discord.check_servers")) {
            scope = "identify guilds";
        } else {
            scope = "identify";
        }


        JavalinLogger.enabled = false;
        app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.addStaticFiles("/web", Location.CLASSPATH);
            config.requestLogger((ctx, timeMs) -> LoggerSpigot.info(String.format("[Webserver] %s %s %s took %s ms ", ctx.status(), ctx.method(), ctx.path(), timeMs), true));
        });


        app.get("/auth", this::Oauth);
        app.get("/callback", this::OauthCallback);

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
            if(this.Spigot != null) LoggerSpigot.log("[Webserver] Stopped", true);
            else LoggerBungee.log("[Webserver] Stopped", true);
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

    private JSONObject getUser(JSONObject oauth){
        Request req = new Request.Builder().url("https://discord.com/api/v9/users/@me")
                .addHeader("Authorization", "Bearer " + oauth.getString("access_token")).build();

        try (Response resp = httpClient.newCall(req).execute()) {
            if (resp.code() == 401) {
                return null;
            }
            ResponseBody body = resp.body();
            if (body == null) {
                return null;
            }
            JSONObject user = new JSONObject(new JSONTokener(body.string()));

            return user;
//            return new SavedOAuthUser(user.getString("username"), user.getString("discriminator"),
//                    user.getString("id"));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void Oauth(@NotNull Context ctx) {

        String code = ctx.queryParam("code");

        if(code == null || code.length() == 0){
            ctx.render("/web/code.html");
            return;
        }

        if(!getCodes().containsKey(code)) {
            ctx.render("/web/codenotfound.html");
            return;
        }

        UUID puid = getCodes().get(code);
        String state = UUID.randomUUID().toString();
        this.states.put(state, puid);

        if(this.Spigot != null){

            AuthURL = UrlBuilder.fromString("https://discord.com/api/oauth2/authorize")
                    .addParameter("prompt", "consent")
                    .addParameter("client_id", client_id)
                    .addParameter("scope", scope)
                    .addParameter("redirect_uri", redirect_uri)
                    .addParameter("response_type", "code")
                    .addParameter("state", state);

            ctx.redirect(AuthURL.toString(), 307);

        }else{

            AuthURL = UrlBuilder.fromString("https://discord.com/api/oauth2/authorize")
                    .addParameter("prompt", "consent")
                    .addParameter("client_id", client_id)
                    .addParameter("scope", scope)
                    .addParameter("redirect_uri", redirect_uri)
                    .addParameter("response_type", "code")
                    .addParameter("state", state);

            ctx.redirect(AuthURL.toString(), 307);
        }


    }


    private void OauthCallback(@NotNull Context ctx) {
        //Do something here to take the code, given as a pram and yeet yeet it to get a barrer/refresh token
        String code = ctx.queryParam("code");
        String state = ctx.queryParam("state");

        if(state == null || !this.states.containsKey(state)){
            ctx.redirect("/notvalidauth", 307);
        }

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

            JSONObject user = getUser(bodyJson);

            if(user == null){
                ctx.result("Unable to get Discord user details.");
            }

            String key = "users." + states.get(state).toString() + ".";

            if(this.Spigot != null){
                return;
            }else{

                this.Bungee.getAuthConfig().getConfig().set(key + "user_id", user.getLong("id"));
                this.Bungee.getAuthConfig().getConfig().set(key + "username", user.getString("username"));
                this.Bungee.getAuthConfig().getConfig().set(key + "discriminator", user.getInt("discriminator"));
                this.Bungee.getAuthConfig().getConfig().set(key + "auth_time", new Timestamp(System.currentTimeMillis()).toString());
                this.Bungee.getAuthConfig().getConfig().set(key + "access_token", bodyJson.getString("access_token"));
                this.Bungee.getAuthConfig().getConfig().set(key + "refresh_token", bodyJson.getString("refresh_token"));
                this.Bungee.getAuthConfig().getConfig().set(key + "expires_in", bodyJson.getInt("expires_in"));
                this.Bungee.getAuthConfig().getConfig().set(key + "auth_time_ms", System.currentTimeMillis());
                this.Bungee.getAuthConfig().save();


            }

            ctx.result("Link Successful. Welcome " + user.getString("username") + "#" + user.getString("discriminator") + " (" + user.getString("id") + ")");
            return;

        } catch (Exception e) {
            e.printStackTrace();
        }

        ctx.result("Linking error has occurred. Please contact the developer.");
    }

    public Map<String, UUID> getCodes() { return codes; }

}