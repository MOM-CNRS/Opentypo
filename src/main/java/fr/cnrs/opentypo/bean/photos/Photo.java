package fr.cnrs.opentypo.bean.photos;

import lombok.Data;
import java.io.Serializable;
import java.util.UUID;

@Data
public class Photo implements Serializable {

    private String id;
    private String itemImageSrc;
    private String thumbnailImageSrc;
    private String alt;
    private String title;

    public Photo() {
        this.id = UUID.randomUUID().toString().substring(0, 8);
    }

    public Photo(String itemImageSrc, String thumbnailImageSrc, String alt, String title) {
        this();
        this.itemImageSrc = itemImageSrc;
        this.thumbnailImageSrc = thumbnailImageSrc;
        this.alt = alt;
        this.title = title;
    }
}