package pbjsystems.checkForMissingDayService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.reactive.function.client.WebClient;
import pbjsystems.checkForMissingDayService.model.Holiday;
import pbjsystems.checkForMissingDayService.model.States;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;


@org.springframework.stereotype.Service
public class Service {
    private static final Logger logger = LoggerFactory.getLogger(Service.class);
    @Autowired
    SpringJdbcConfig config;

    @Scheduled(cron = "0 0 3 * * ?")
    public void checkingForMissingUserDay() {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String yesterday = dateFormat.format(cal.getTime());
        if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY || checkForHoliday(yesterday, States.he)) {
            return;
        } else {
            addUserMissingDay(yesterday);
            subtractUserSaldo(yesterday);
        }
    }

    private void addUserMissingDay(String yesterday) {
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(config.mysqlDataSource()).withProcedureName("add_user_missing_days");
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("yesterday", yesterday);
        SqlParameterSource in = new MapSqlParameterSource(parameter);
        simpleJdbcCall.execute(in);
    }

    private void subtractUserSaldo(String yesterday) {
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(config.mysqlDataSource()).withProcedureName("subtract_user_saldo");
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("yesterday", yesterday);
        SqlParameterSource in = new MapSqlParameterSource(parameter);
        simpleJdbcCall.execute(in);
    }

    private boolean checkForHoliday(String date, States state) {
        String url = "https://deutsche-feiertage-api.de/api/v1/" + date + "?short=true&bundeslaender=" + state;
        WebClient client = WebClient
                .builder()
                .baseUrl(url)
                .defaultHeader("X-DFA-Token", "dfa")
                .build();
        try {
            Holiday holiday = client.post().retrieve().bodyToMono(Holiday.class).block();
            return holiday.isHoliday();
        } catch(Exception e) {
            logger.info("Feiertage request failed. Please check if this api is still usable! ");
            return true;
        }
    }
}
