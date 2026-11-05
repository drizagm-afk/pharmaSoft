package com.edu.upeu.PharmaBackend;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.edu.upeu.PharmaBackend.config.RequestIdFilter;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.*;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.*;

class RequestIdFilterTest {
    private final RequestIdFilter filter = new RequestIdFilter();
    private final Logger logger = (Logger) LoggerFactory.getLogger(RequestIdFilter.class);
    private ListAppender<ILoggingEvent> appender;
    private Level anterior;

    @BeforeEach
    void preparar() {
        anterior = logger.getLevel(); logger.setLevel(Level.INFO);
        appender = new ListAppender<>() {
            @Override protected void append(ILoggingEvent event) {
                // Snapshot MDC before the filter clears the request thread.
                event.prepareForDeferredProcessing();
                super.append(event);
            }
        };
        appender.start(); logger.addAppender(appender);
    }

    @AfterEach
    void limpiar() {
        logger.detachAppender(appender); appender.stop(); logger.setLevel(anterior); MDC.clear();
    }

    @Test
    void idCoincideEnCabeceraContextoYLogSinPropagarseALaSiguientePeticion() throws Exception {
        String anteriorId = null;
        for (int estado : new int[]{200, 401, 403}) {
            var request = new MockHttpServletRequest("GET", "/api/v1/categorias");
            request.addHeader(RequestIdFilter.CABECERA, "id-no-confiable");
            var response = new MockHttpServletResponse();
            filter.doFilter(request, response, (req, res) -> {
                assertThat(MDC.get("requestId")).isEqualTo(response.getHeader(RequestIdFilter.CABECERA));
                response.setStatus(estado);
            });
            String id = response.getHeader(RequestIdFilter.CABECERA);
            assertThat(id).matches("[a-f0-9]{8}").isNotEqualTo(anteriorId);
            ILoggingEvent evento = appender.list.getLast();
            assertThat(evento.getMDCPropertyMap()).containsEntry("requestId", id);
            assertThat(evento.getFormattedMessage()).contains("GET /api/v1/categorias -> " + estado);
            assertThat(MDC.get("requestId")).isNull();
            anteriorId = id;
        }
    }

    @Test
    void excepcionSePropagaYRegistra500SinDejarMdc() {
        var response = new MockHttpServletResponse();
        assertThatThrownBy(() -> filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/prueba"),
                response, (req, res) -> { throw new ServletException("Error de prueba"); }))
                .isInstanceOf(ServletException.class);
        assertThat(appender.list.getLast().getFormattedMessage()).contains("-> 500");
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void noRegistraCuerposCabecerasNiParametros() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setQueryString("password=secreto-query");
        request.addHeader("Authorization", "Bearer secreto-token");
        request.setContent("{\"password\":\"secreto-body\"}".getBytes());
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {});
        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.getFirst().getFormattedMessage())
                .contains("POST /api/v1/auth/login -> 200")
                .doesNotContain("password", "Bearer", "secreto");
    }

    @Test
    void monitoreoRecibeIdPeroNoGeneraLogDeAcceso() throws Exception {
        var response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/actuator/health"), response, (req, res) -> {});
        assertThat(response.getHeader(RequestIdFilter.CABECERA)).matches("[a-f0-9]{8}");
        assertThat(appender.list).isEmpty();
        assertThat(MDC.get("requestId")).isNull();
    }
}
