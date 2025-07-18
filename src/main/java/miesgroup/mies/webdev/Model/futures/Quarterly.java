package miesgroup.mies.webdev.Model.futures;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "quarterly_futures")
public class Quarterly extends PanacheEntityBase {
    @Id
    private Integer id;

    private Integer year;

    private Integer quarter;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    public Futures future;

    public Quarterly() {}
    public Quarterly(final Futures future, final Integer year, final Integer quarter) {
        this.future = future;
        this.year = year;
        this.quarter = quarter;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getQuarter() {
        return quarter;
    }

    public void setQuarter(Integer quarter) {
        this.quarter = quarter;
    }

    public Futures getFuture() {
        return future;
    }

    public void setFuture(Futures future) {
        this.future = future;
    }

    @Override
    public String toString() {
        return "Quarterly{" +
                "id=" + id +
                ", year=" + year +
                ", quarter=" + quarter +
                ", future=" + future +
                '}';
    }
}
