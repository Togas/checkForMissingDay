package pbjsystems.newyearservice.model;

public class Holiday {
    private boolean isHoliday;
    private boolean isGeneral;
    private String name;

    public boolean isHoliday() {
        return isHoliday;
    }

    public void setHoliday(boolean isHoliday) {
        this.isHoliday = isHoliday;
    }

    public boolean isGeneral() {
        return isGeneral;
    }

    public void setGeneral(boolean isGeneral) {
        this.isGeneral = isGeneral;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
