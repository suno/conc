package ru.suno.concordance;

public class ConcRef {
    private int pageNumber;
    
    private int volumeNumber;
    
    public ConcRef(int volume, int page) {
        setVolumeNumber(volume);
        setPageNumber(page);        
    }
    
    public int getPageNumber() {
        return pageNumber;
    }
    public void setPageNumber(int pgNum) {
        this.pageNumber = pgNum;
    }
    public int getVolumeNumber() {
        return volumeNumber;
    }
    public void setVolumeNumber(int volNum) {
        this.volumeNumber = volNum;
    }
}
