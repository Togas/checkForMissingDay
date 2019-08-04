package pbjsystems.checkForMissingDayService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
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

    @Scheduled(fixedRate = 1000)
    public void checkingForMissingUserDay() {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String yesterday = dateFormat.format(cal.getTime());
        if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY || checkForHoliday(yesterday, States.he)) {
            return;
        } else {
        yesterday = "'" + yesterday + "'";
        addUserMissingDay(yesterday);
        subtractUserSaldo(yesterday);
        }
    }

    private void addUserMissingDay(String yesterday) {
//        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(config.mysqlDataSource());
//        Map<String, Object> parameter = new HashMap<>();
//        parameter.put("yesterday", yesterday);
//        SqlParameterSource in = new MapSqlParameterSource(parameter);
//        simpleJdbcCall.execute(in);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(config.mysqlDataSource());
        jdbcTemplate.execute("INSERT into user_missing_days(user_id, missing_date)" +
                "SELECT id," + yesterday +
                "FROM users " +
                "WHERE is_deleted=0 and " +
                "id in" +
                "(SELECT id " +
                "FROM users " +
                "WHERE id not IN" +
                "(SELECT users_id " +
                "FROM USER_timesheets " +
                "WHERE DATE=" + yesterday + ") " +
                "and id NOT IN" +
                "(SELECT user_id " +
                "FROM vacation " +
                "WHERE date_from<=" + yesterday + " AND date_to>=" + yesterday + ")" +
                "and id NOT IN" +
                "(SELECT user_id " +
                "FROM disease" +
                " WHERE date_from<=" + yesterday + " AND date_to>=" + yesterday + "));");
    }

    private void subtractUserSaldo(String yesterday) {
//        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(config.mysqlDataSource()).withProcedureName("subtract_user_saldo");
//        Map<String, Object> parameter = new HashMap<>();
//        parameter.put("yesterday", yesterday);
//        SqlParameterSource in = new MapSqlParameterSource(parameter);
//        simpleJdbcCall.execute(in);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(config.mysqlDataSource());
        jdbcTemplate.execute("update users " +
                "SET saldo_total=saldo_total - weekly_work_hrs/5 " +
                "WHERE is_deleted=0 and " +
                "id in" +
                "(SELECT id " +
                "FROM users " +
                "WHERE id not IN" +
                "(SELECT users_id " +
                "FROM USER_timesheets " +
                "WHERE DATE=" + yesterday + ") " +
                "and id NOT IN" +
                "(SELECT user_id " +
                "FROM vacation " +
                "WHERE date_from<=" + yesterday + " AND date_to>=" + yesterday + ")" +
                "and id NOT IN" +
                "(SELECT user_id " +
                "FROM disease" +
                " WHERE date_from<=" + yesterday + " AND date_to>=" + yesterday + "));");

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
        } catch (Exception e) {
            logger.info("Feiertage request failed. Please check if this api is still usable! ");
            return true;
        }
    }
}
