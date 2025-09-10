package model;

public class PhoneticTraits {
    private PhonemeType type;
    private int sonority;
    
    public PhoneticTraits(PhonemeType type, int sonority) {
        this.type = type;
        this.sonority = sonority;
    }

    public PhonemeType getType() {
        return type;
    }

    public int getSonority() {
        return sonority;
    }

    public void setType(PhonemeType type) {
        this.type = type;
    }

    public void setSonority(int sonority) {
        this.sonority = sonority;
    }
}
