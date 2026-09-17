package cn.tpl.opc.auth;

import cn.tpl.opc.commons.dto.ResultDTO;
import cn.tpl.opc.controller.SseController;
import cn.tpl.opc.service.impl.SseServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.Cookie;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes=AuthIntegrationTest.TestApp.class, properties={
        "spring.datasource.url=jdbc:h2:mem:auth_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.type=com.zaxxer.hikari.HikariDataSource",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.sentinel.enabled=false"
})
@AutoConfigureMockMvc
public class AuthIntegrationTest {
    @SpringBootConfiguration @EnableAutoConfiguration(exclude=com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceAutoConfigure.class)
    @Import({AuthService.class, LoginAttempts.class, SecurityConfig.class, AuthCookies.class,
            AuthController.class, UserController.class, AuthResponses.class, ProbeController.class,
            SseServiceImpl.class, SseController.class})
    public static class TestApp {}

    @RestController public static class ProbeController {
        @RequestMapping("/**") public ResultDTO<?> probe() { return ResultDTO.success("authorized"); }
    }
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AuthService service;
    @Autowired private LoginAttempts attempts;
    @Autowired private SseServiceImpl sse;
    private final ObjectMapper json = new ObjectMapper();
    private Cookie csrf;
    private String csrfValue;

    @Before public void setup() throws Exception {
        jdbc.update("DELETE FROM sys_user_session");
        jdbc.update("DELETE FROM sys_builtin_session");
        attempts.succeeded(BuiltInAdministrator.USERNAME);
        sse.checkSessions();
        jdbc.update("DELETE FROM sys_auth_audit");
        jdbc.update("DELETE FROM sys_user");
        service.initialize();
        MvcResult result = mvc.perform(get("/auth/csrf")).andReturn();
        assertEquals(200, result.getResponse().getStatus());
        csrfValue = body(result).path("data").path("token").asText();
        csrf = new Cookie("XSRF-TOKEN", csrfValue);
    }

    private JsonNode body(MvcResult result) throws Exception { return json.readTree(result.getResponse().getContentAsString()); }
    private MvcResult send(String method, String path, Cookie session, Object payload, boolean withCsrf) throws Exception {
        MockHttpServletRequestBuilder request = org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(org.springframework.http.HttpMethod.valueOf(method), path);
        request.cookie(csrf);
        if (session != null) request.cookie(session);
        if (withCsrf) request.header("X-XSRF-TOKEN", csrfValue);
        if (payload != null) request.contentType("application/json").content(json.writeValueAsString(payload));
        return mvc.perform(request).andReturn();
    }
    private Cookie login(String username, String password) throws Exception {
        MvcResult result = send("POST", "/auth/login", null, Map.of("username", username, "password", password), true);
        assertEquals(result.getResponse().getContentAsString(), 200, result.getResponse().getStatus());
        String header = result.getResponse().getHeaders("Set-Cookie").stream().filter(value -> value.startsWith(AuthCookies.NAME + "=")).findFirst().orElseThrow();
        assertTrue(header.contains("HttpOnly")); assertTrue(header.contains("SameSite=Lax"));
        return new Cookie(AuthCookies.NAME, header.substring(header.indexOf('=') + 1, header.indexOf(';')));
    }
    private Cookie admin() throws Exception {
        Cookie first = login("admin", "example-admin-password");
        assertEquals(200, send("POST", "/auth/password", first, Map.of("oldPassword", "example-admin-password", "newPassword", "Admin-test-123"), true).getResponse().getStatus());
        return login("admin", "Admin-test-123");
    }
    private long userId(String name) { return jdbc.queryForObject("SELECT id FROM sys_user WHERE username=?", Long.class, name); }
    private Cookie reader(Cookie admin) throws Exception {
        assertEquals(200, send("POST", "/users", admin, Map.of("username", "reader", "password", "Reader-first-123", "role", "USER"), true).getResponse().getStatus());
        Cookie first = login("reader", "Reader-first-123");
        assertEquals(200, send("POST", "/auth/password", first, Map.of("oldPassword", "Reader-first-123", "newPassword", "Reader-test-123"), true).getResponse().getStatus());
        return login("reader", "Reader-test-123");
    }

    @Test public void anonymousIsDeniedIncludingLegacyWritesSseAndManagement() throws Exception {
        for (String path : List.of("/cushion/cushionsPage", "/users", "/sse/devicesStatus/1", "/device/deviceConnections/1", "/druid/index.html", "/actuator/env", "/test/druidStat")) {
            assertEquals(path, 401, send("GET", path, null, null, false).getResponse().getStatus());
        }
        assertEquals(401, send("POST", "/cushion/manualCushionInfo", null, Map.of(), false).getResponse().getStatus());
        assertEquals(200, send("GET", "/actuator/health", null, null, false).getResponse().getStatus());
        assertEquals(401, send("GET", "/auth/me", new Cookie(AuthCookies.NAME,"forged"), null, false).getResponse().getStatus());
    }

    @Test public void firstLoginAllowsAccessAndOptionalPasswordChangeRevokesAllSessions() throws Exception {
        Cookie first = login("admin","example-admin-password"), second = login("admin","example-admin-password");
        assertFalse(body(send("GET","/auth/me",first,null,false)).path("data").path("mustChangePassword").asBoolean());
        assertEquals(200, send("GET","/users",first,null,false).getResponse().getStatus());
        assertEquals(400, send("POST","/auth/password",first,Map.of("oldPassword","wrong","newPassword","valid-new-pass"),true).getResponse().getStatus());
        assertEquals(400, send("POST","/auth/password",first,Map.of("oldPassword","example-admin-password","newPassword","example-admin-password"),true).getResponse().getStatus());
        assertEquals(200, send("POST","/auth/password",first,Map.of("oldPassword","example-admin-password","newPassword","Admin-test-123"),true).getResponse().getStatus());
        for (Cookie cookie : List.of(first,second)) assertEquals(401,send("GET","/auth/me",cookie,null,false).getResponse().getStatus());
        Cookie current = login("admin","Admin-test-123");
        assertFalse(body(send("GET","/auth/me",current,null,false)).path("data").path("mustChangePassword").asBoolean());
        String stored = jdbc.queryForObject("SELECT password_hash FROM sys_user WHERE username='admin'",String.class);
        assertTrue(stored.startsWith("$2")); assertNotEquals("Admin-test-123",stored);
        assertFalse(jdbc.queryForList("SELECT * FROM sys_auth_audit").toString().contains("Admin-test-123"));
    }

    @Test public void ordinaryUserCanQueryAndExportButAllWriteRoutesAreDenied() throws Exception {
        Cookie admin=admin(), reader=reader(admin);
        String[] reads={"/cushion/cushionsPage","/cushion/cushions","/cushion/cushions/qr","/cushion/detailsPage","/cushion/details/excel",
                "/device/devicesPage","/device/list","/device/devicesStatus/1","/device/1","/deviceInstallPositions",
                "/deviceInstallPositions/page","/deviceInstallPositions/1","/plcAddr/page","/plcAddr/1","/opcConfig",
                "/opcConfig/page","/options/deviceTypes","/options/plcAddrTypes","/options/deviceInstallPositions",
                "/options/plcDevices","/options/scannerDevices","/scanLogs","/operationEvents/recent"};
        for(String path:reads) assertEquals(path,200,send("GET",path,reader,null,false).getResponse().getStatus());
        assertEquals(200,send("POST","/cushion/cushions/excel",reader,List.of(1),true).getResponse().getStatus());
        String[][] writes={{"POST","/cushion/manualCushionInfo"},{"POST","/cushion/manualCushionInfo/1/qr"},{"POST","/cushion/cushions"},
                {"POST","/device"},{"DELETE","/device/1"},{"DELETE","/device/connection/1"},{"GET","/device/deviceConnections/1"},
                {"GET","/device/scannerConnections"},{"POST","/plcAddr"},{"DELETE","/plcAddr/1"},{"POST","/opcConfig"},
                {"DELETE","/opcConfig/1"},{"POST","/deviceInstallPositions"},{"DELETE","/deviceInstallPositions/1"},
                {"GET","/users"},{"POST","/users"},{"PUT","/users/1"},{"POST","/users/1/password"}};
        for(String[] item:writes) assertEquals(Arrays.toString(item),403,send(item[0],item[1],reader,Map.of(),true).getResponse().getStatus());
        for(String[] item:writes) if(!item[1].startsWith("/users")) assertEquals(Arrays.toString(item),200,send(item[0],item[1],admin,Map.of(),true).getResponse().getStatus());
        assertEquals(403,send("GET","/test/testPLCAddr",admin,null,false).getResponse().getStatus());
        assertFalse(send("GET","/users",admin,null,false).getResponse().getContentAsString().contains("password_hash"));
    }

    @Test public void csrfRequiredForLoginAndWritesAndHostileCorsIsRejected() throws Exception {
        assertEquals(403,send("POST","/auth/login",null,Map.of("username","admin","password","example-admin-password"),false).getResponse().getStatus());
        Cookie admin=admin();
        assertEquals("CSRF_INVALID",body(send("POST","/opcConfig",admin,Map.of(),false)).path("reason").asText());
        assertEquals("CSRF_INVALID",body(send("GET","/device/deviceConnections/1",admin,null,false)).path("reason").asText());
        assertEquals(200,send("GET","/opcConfig",admin,null,false).getResponse().getStatus());
        assertEquals(403,mvc.perform(options("/users").header("Origin","https://untrusted.example").header("Access-Control-Request-Method","POST")).andReturn().getResponse().getStatus());
    }

    @Test public void logoutRevokesOnlyCurrentBrowserAndClosesItsSse() throws Exception {
        Cookie admin=admin(), another=login("admin","Admin-test-123");
        MvcResult stream=send("GET","/sse/devicesStatus/1",admin,null,false);
        assertTrue(stream.getRequest().isAsyncStarted());
        assertEquals(200,send("POST","/auth/logout",admin,null,true).getResponse().getStatus());
        assertEquals(401,send("GET","/auth/me",admin,null,false).getResponse().getStatus());
        assertEquals(200,send("GET","/auth/me",another,null,false).getResponse().getStatus());
        stream.getAsyncResult(2000); // Completion has a null result; this throws if the stream remains open.
    }

    @Test public void userManagementRevokesSessionsAndProtectsLastAdmin() throws Exception {
        Cookie admin=admin(), reader=reader(admin);
        long id=userId("reader");
        assertEquals(200,send("PUT","/users/"+id,admin,Map.of("role","ADMIN","enabled",true),true).getResponse().getStatus());
        assertEquals(401,send("GET","/auth/me",reader,null,false).getResponse().getStatus());
        Cookie promoted=login("reader","Reader-test-123");
        assertEquals(200,send("GET","/users",promoted,null,false).getResponse().getStatus());
        assertEquals(200,send("PUT","/users/"+id,admin,Map.of("role","USER","enabled",false),true).getResponse().getStatus());
        assertEquals(401,send("POST","/auth/login",null,Map.of("username","reader","password","Reader-test-123"),true).getResponse().getStatus());
        assertEquals(400,send("PUT","/users/"+userId("admin"),admin,Map.of("role","USER","enabled",true),true).getResponse().getStatus());
        assertEquals(400,send("PUT","/users/"+userId("admin"),admin,Map.of("role","ADMIN","enabled",false),true).getResponse().getStatus());
    }

    @Test public void resetPasswordInvalidatesExistingLoginAndRequiresChange() throws Exception {
        Cookie admin=admin(), reader=reader(admin);
        assertEquals(200,send("POST","/users/"+userId("reader")+"/password",admin,Map.of("password","Reset-pass-123"),true).getResponse().getStatus());
        assertEquals(401,send("GET","/auth/me",reader,null,false).getResponse().getStatus());
        Cookie reset=login("reader","Reset-pass-123");
        assertTrue(body(send("GET","/auth/me",reset,null,false)).path("data").path("mustChangePassword").asBoolean());
    }

    @Test public void repeatedInitializationPreservesUsersPasswordsAndSessions() throws Exception {
        Cookie admin=admin();
        service.initialize(); service.initialize();
        assertEquals(1L,(long)jdbc.queryForObject("SELECT COUNT(*) FROM sys_user",Long.class));
        assertEquals(200,send("GET","/auth/me",admin,null,false).getResponse().getStatus());
        assertEquals(64,jdbc.queryForObject("SELECT token_hash FROM sys_user_session",String.class).length());
        assertNotEquals(admin.getValue(),jdbc.queryForObject("SELECT token_hash FROM sys_user_session",String.class));
    }

    @Test public void accountValidationAndLoginThrottling() throws Exception {
        Cookie admin=admin();
        for(String role:List.of("ROOT","admin","")) assertTrue(send("POST","/users",admin,Map.of("username","badrole","password","User-test-123","role",role),true).getResponse().getStatus() >= 400);
        for(int i=0;i<5;i++) assertEquals(401,send("POST","/auth/login",null,Map.of("username","missing"+userId("admin"),"password","bad-pass"),true).getResponse().getStatus());
        assertEquals(429,send("POST","/auth/login",null,Map.of("username","missing"+userId("admin"),"password","bad-pass"),true).getResponse().getStatus());
    }

    @Test public void localRecoveryRevokesAllSessionsAndOnlyRecoversAdministrators() throws Exception {
        Cookie admin=admin(), another=login("admin","Admin-test-123"), reader=reader(admin);
        try { service.recoverAdministrator("reader","Recovery-pass-123"); fail("Must reject ordinary accounts"); }
        catch(AuthException expected) { assertEquals(400,expected.status); }
        service.recoverAdministrator("admin","Recovery-pass-123");
        for(Cookie cookie:List.of(admin,another)) assertEquals(401,send("GET","/auth/me",cookie,null,false).getResponse().getStatus());
        assertEquals(200,send("GET","/auth/me",reader,null,false).getResponse().getStatus());
        Cookie recovered=login("admin","Recovery-pass-123");
        assertTrue(body(send("GET","/auth/me",recovered,null,false)).path("data").path("mustChangePassword").asBoolean());
    }

    @Test public void revokedInFlightPrincipalCannotChangePassword() throws Exception {
        Cookie admin=admin();
        AuthPrincipal stale=service.authenticate(admin.getValue());
        service.logout(stale);
        try { service.changePassword(stale,"Admin-test-123","Unauthorized-new-123"); fail("Revoked principal cannot change password"); }
        catch(AuthException expected) { assertEquals(401,expected.status); }
        assertNotNull(login("admin","Admin-test-123"));
    }

    @Test public void newReaderUsesInitialPasswordWithReadOnlyPermissions() throws Exception {
        Cookie admin=login("admin","example-admin-password");
        assertEquals(200,send("POST","/users",admin,Map.of("username","newreader","password","Initial-reader-123","role","USER"),true).getResponse().getStatus());
        Cookie reader=login("newreader","Initial-reader-123");
        assertFalse(body(send("GET","/auth/me",reader,null,false)).path("data").path("mustChangePassword").asBoolean());
        assertEquals(200,send("GET","/opcConfig/page",reader,null,false).getResponse().getStatus());
        assertEquals(403,send("POST","/opcConfig",reader,Map.of(),true).getResponse().getStatus());
    }

    @Test public void upgradeClearsInitialFlagsButPreservesPasswordResetRequirementsAndSessions() throws Exception {
        Cookie admin=login("admin","example-admin-password");
        service.createUser(service.authenticate(admin.getValue()),"resetuser","Reset-first-123","USER");
        service.resetPassword(service.authenticate(admin.getValue()),userId("resetuser"),"Reset-next-123");
        jdbc.update("UPDATE sys_user SET must_change_password=TRUE WHERE username='admin'");
        String before=jdbc.queryForObject("SELECT password_hash FROM sys_user WHERE username='admin'",String.class);
        service.initialize(); service.initialize();
        assertFalse(body(send("GET","/auth/me",admin,null,false)).path("data").path("mustChangePassword").asBoolean());
        assertEquals(before,jdbc.queryForObject("SELECT password_hash FROM sys_user WHERE username='admin'",String.class));
        Cookie reset=login("resetuser","Reset-next-123");
        assertEquals("PASSWORD_CHANGE_REQUIRED",body(send("GET","/opcConfig/page",reset,null,false)).path("reason").asText());
    }

    @Test public void concurrentAdminDemotionsNeverRemoveLastAdministrator() throws Exception {
        Cookie cookie=admin();
        service.createUser(service.authenticate(cookie.getValue()),"second","Second-test-123","ADMIN");
        Cookie second=login("second","Second-test-123");
        service.changePassword(service.authenticate(second.getValue()),"Second-test-123","Second-new-123");
        second=login("second","Second-new-123");
        AuthPrincipal one=service.authenticate(cookie.getValue()),two=service.authenticate(second.getValue());
        ExecutorService executor=Executors.newFixedThreadPool(2);
        CountDownLatch ready=new CountDownLatch(1);
        try {
            List<Future<?>> futures=new ArrayList<>();
            for(AuthPrincipal actor:List.of(one,two)) futures.add(executor.submit(() -> {
                try { ready.await(); service.updateUser(actor,actor.getId(),"USER",true); }
                catch(AuthException expected) { assertTrue(expected.status==400 || expected.status==403); }
                catch(InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException(e); }
            }));
            ready.countDown();
            for(Future<?> future:futures) future.get(10,TimeUnit.SECONDS);
            assertEquals(1L,(long)jdbc.queryForObject("SELECT COUNT(*) FROM sys_user WHERE enabled=TRUE AND role='ADMIN'",Long.class));
        } finally { executor.shutdownNow(); }
    }

    @Test public void builtInLoginHasNoDatabaseAccountOrCredentialAndSurvivesInitialization() throws Exception {
        Cookie root = login("superadmin", "example-maintenance-password-1");
        JsonNode profile = body(send("GET", "/auth/me", root, null, false)).path("data");
        assertEquals(-1, profile.path("id").asLong());
        assertEquals("ADMIN", profile.path("role").asText());
        assertTrue(profile.path("builtIn").asBoolean());
        assertFalse(profile.path("passwordChangeAllowed").asBoolean());
        assertFalse(profile.path("mustChangePassword").asBoolean());
        assertEquals(0L, (long) jdbc.queryForObject("SELECT COUNT(*) FROM sys_user WHERE username='superadmin'", Long.class));
        assertEquals(0L, (long) jdbc.queryForObject("SELECT COUNT(*) FROM sys_user_session", Long.class));
        assertEquals(service.authenticate(root.getValue()).getSessionHash(), jdbc.queryForObject("SELECT token_hash FROM sys_builtin_session", String.class));
        assertFalse(jdbc.queryForList("SELECT * FROM sys_auth_audit").toString().contains("example-maintenance-password-1"));
        service.initialize();
        jdbc.update("UPDATE sys_user SET enabled=FALSE");
        assertEquals(200, send("GET", "/users", root, null, false).getResponse().getStatus());
        assertNotNull(login("superadmin", "example-maintenance-password-1"));
    }

    @Test public void builtInPasswordIdentityAndRoleCannotBeChangedThroughAnyMaintenancePath() throws Exception {
        Cookie root = login("superadmin", "example-maintenance-password-1"), admin = login("admin", "example-admin-password");
        assertEquals(403, send("POST", "/auth/password", root, Map.of("oldPassword", "example-maintenance-password-1", "newPassword", "Changed-pass-123"), true).getResponse().getStatus());
        for (Cookie actor : List.of(root, admin)) {
            assertEquals(403, send("POST", "/users/-1/password", actor, Map.of("password", "Changed-pass-123"), true).getResponse().getStatus());
            assertEquals(403, send("PUT", "/users/-1", actor, Map.of("role", "USER", "enabled", false), true).getResponse().getStatus());
            assertEquals(403, send("POST", "/users", actor, Map.of("username", "superadmin", "password", "Other-pass-123", "role", "ADMIN"), true).getResponse().getStatus());
        }
        try { service.recoverAdministrator("superadmin", "Changed-pass-123"); fail("Built-in recovery must be rejected"); }
        catch (AuthException expected) { assertEquals(403, expected.status); }
        assertEquals(401, send("POST", "/auth/login", null, Map.of("username", "superadmin", "password", "Changed-pass-123"), true).getResponse().getStatus());
        assertNotNull(login("superadmin", "example-maintenance-password-1"));
    }

    @Test public void builtInCanRestoreAdminAndManageUsersWithAuditAndSessionRevocation() throws Exception {
        Cookie oldAdmin = login("admin", "example-admin-password"), root = login("superadmin", "example-maintenance-password-1");
        assertEquals(200, send("POST", "/users/" + userId("admin") + "/password", root, Map.of("password", "Restored-admin-123"), true).getResponse().getStatus());
        assertEquals(401, send("GET", "/auth/me", oldAdmin, null, false).getResponse().getStatus());
        Cookie restored = login("admin", "Restored-admin-123");
        assertEquals("PASSWORD_CHANGE_REQUIRED", body(send("GET", "/users", restored, null, false)).path("reason").asText());
        assertEquals(200, send("POST", "/auth/password", restored, Map.of("oldPassword", "Restored-admin-123", "newPassword", "Admin-final-123"), true).getResponse().getStatus());
        assertEquals(200, send("GET", "/users", login("admin", "Admin-final-123"), null, false).getResponse().getStatus());
        assertEquals(200, send("POST", "/users", root, Map.of("username", "builtinreader", "password", "Reader-test-123", "role", "USER"), true).getResponse().getStatus());
        assertEquals(200, send("PUT", "/users/" + userId("builtinreader"), root, Map.of("role", "USER", "enabled", false), true).getResponse().getStatus());
        assertEquals("superadmin", jdbc.queryForObject("SELECT actor FROM sys_auth_audit WHERE action='RESET_PASSWORD' AND target_username='admin'", String.class));
        assertEquals(200, send("POST", "/opcConfig", root, Map.of(), true).getResponse().getStatus());
    }

    @Test public void renamedBuiltInRejectsOldCredentialsAndLegacyCookies() throws Exception {
        assertEquals(401, send("POST", "/auth/login", null, Map.of("username", "superamin", "password", "superamin123"), true).getResponse().getStatus());
        assertEquals(401, send("POST", "/auth/login", null, Map.of("username", "superadmin", "password", "superamin123"), true).getResponse().getStatus());
        Cookie legacy = new Cookie(AuthCookies.NAME, "B".repeat(43));
        jdbc.update("INSERT INTO sys_builtin_session(token_hash) VALUES(?)", AuthService.hash(legacy.getValue()));
        assertNull(service.authenticate(legacy.getValue()));
        Cookie current = login("superadmin", "example-maintenance-password-1");
        service.initialize();
        assertNotNull(service.authenticate(current.getValue()));
    }

    @Test public void builtInLogoutClosesOnlyItsSseAndRevokedPrincipalCannotManageUsers() throws Exception {
        Cookie root = login("superadmin", "example-maintenance-password-1"), other = login("superadmin", "example-maintenance-password-1");
        AuthPrincipal stale = service.authenticate(root.getValue());
        MvcResult stream = send("GET", "/sse/devicesStatus/1", root, null, false);
        assertTrue(stream.getRequest().isAsyncStarted());
        assertEquals(200, send("POST", "/auth/logout", root, null, true).getResponse().getStatus());
        stream.getAsyncResult(2000);
        assertFalse(service.isSessionValid(stale.getSessionHash()));
        assertEquals(401, send("GET", "/auth/me", root, null, false).getResponse().getStatus());
        assertEquals(200, send("GET", "/auth/me", other, null, false).getResponse().getStatus());
        try { service.resetPassword(stale, userId("admin"), "Stale-pass-123"); fail("Revoked session must fail"); }
        catch (AuthException expected) { assertEquals(403, expected.status); }
    }

    @Test public void databaseNamesakeCannotOverrideOrImpersonateBuiltInIdentity() throws Exception {
        jdbc.update("INSERT INTO sys_user(username,password_hash,role) SELECT 'superadmin',password_hash,'ADMIN' FROM sys_user WHERE username='admin'");
        Cookie dbSession = new Cookie(AuthCookies.NAME, "A".repeat(43));
        jdbc.update("INSERT INTO sys_user_session(token_hash,user_id) VALUES(?,?)", AuthService.hash(dbSession.getValue()), userId("superadmin"));
        assertEquals(401, send("POST", "/auth/login", null, Map.of("username", "superadmin", "password", "example-admin-password"), true).getResponse().getStatus());
        assertEquals(401, send("GET", "/auth/me", dbSession, null, false).getResponse().getStatus());
        assertFalse(service.isSessionValid(AuthService.hash(dbSession.getValue())));
        Cookie root = login("superadmin", "example-maintenance-password-1");
        assertEquals(403, send("POST", "/users/" + userId("superadmin") + "/password", root, Map.of("password", "Other-pass-123"), true).getResponse().getStatus());
        assertTrue(service.authenticate(root.getValue()).isBuiltIn());
    }

    @Test public void builtInStillRequiresCsrfAndLoginThrottling() throws Exception {
        assertEquals(403, send("POST", "/auth/login", null, Map.of("username", "superadmin", "password", "example-maintenance-password-1"), false).getResponse().getStatus());
        Cookie root = login("superadmin", "example-maintenance-password-1");
        assertEquals("CSRF_INVALID", body(send("POST", "/users/" + userId("admin") + "/password", root, Map.of("password", "Csrf-pass-123"), false)).path("reason").asText());
        for (int i = 0; i < 5; i++) {
            try { service.login("superadmin", "wrong-pass", "builtin-throttle-test"); fail("Wrong password accepted"); }
            catch (AuthException expected) { assertEquals(401, expected.status); }
        }
        try { service.login("superadmin", "example-maintenance-password-1", "builtin-throttle-test"); fail("Throttled account accepted"); }
        catch (AuthException expected) { assertEquals(429, expected.status); }
        finally { attempts.succeeded("superadmin"); }
    }
}
