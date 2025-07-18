package miesgroup.mies.webdev.Model.futures;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "futures")
public class Futures extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Temporal(TemporalType.DATE)
    private LocalDate date;

    private Double settlementPrice;

    public Futures() {}

    public Futures(final LocalDate date, final Double settlementPrice) {}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Double getSettlementPrice() {
        return settlementPrice;
    }

    public void setSettlementPrice(Double settlementPrice) {
        this.settlementPrice = settlementPrice;
    }

    @Override
    public String toString() {
        return "Futures{" +
                "id=" + id +
                ", date=" + date +
                ", settlementPrice=" + settlementPrice +
                '}';
    }
}