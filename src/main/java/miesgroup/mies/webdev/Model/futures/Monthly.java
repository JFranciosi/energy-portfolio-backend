package miesgroup.mies.webdev.Model.futures;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "monthly_futures")
public class Monthly extends PanacheEntityBase {
    @Id
    private Integer id;

    private Integer year;
    private Integer month;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    private Futures future;

    public Monthly(){}
    public Monthly(final Futures future, final Integer year, final Integer month){
        this.future = future;
        this.year = year;
        this.month = month;
    }

    public Integer getId() {
        return id;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Futures getFuture() {
        return future;
    }

    public void setFuture(Futures future) {
        this.future = future;
    }

    @Override
    public String toString() {
        return "Monthly{" +
                "id=" + id +
                ", year=" + year +
                ", month=" + month +
                ", future=" + future +
                '}';
    }
}
