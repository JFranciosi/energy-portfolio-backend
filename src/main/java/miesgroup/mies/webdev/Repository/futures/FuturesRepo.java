package miesgroup.mies.webdev.Repository.futures;

import jakarta.enterprise.context.ApplicationScoped;
import miesgroup.mies.webdev.Model.futures.Monthly;
import miesgroup.mies.webdev.Model.futures.Quarterly;
import miesgroup.mies.webdev.Model.futures.Yearly;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class FuturesRepo {

    public List<Yearly> findByYear(String date) {
        LocalDate localDate = LocalDate.parse(date);
        return Yearly.find("future.date", localDate).list();
    }

    public List<Quarterly> findByQuarter(String date) {
        LocalDate localDate = LocalDate.parse(date);
        return Quarterly.find("future.date", localDate).list();
    }

    public List<Monthly> findByMonth(String date) {
        LocalDate localDate = LocalDate.parse(date);
        return Monthly.find("future.date", localDate).list();
    }

    public String getLastDateFromYearlyFutures() {
        return Yearly.findAll()
                .<Yearly>stream()
                .map(y -> y.getFuture().getDate())
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .map(LocalDate::toString)
                .orElse(null);
    }
}
