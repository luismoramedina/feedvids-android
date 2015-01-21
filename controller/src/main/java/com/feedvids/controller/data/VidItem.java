package com.feedvids.controller.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author luismoramedina
 */
public class VidItem implements Comparable {

    public String id;
    public String description;
    public String videoDescription;
    public String itemId;
    public Long dateAdded;
    public Integer order;

    @Override
    public String toString() {
        return "VidItem{" +
                "id='" + id + '\'' +
                ", description='" + description + '\'' +
                ", videoDescription='" + videoDescription + '\'' +
                ", itemId='" + itemId + '\'' +
                ", dateAdded=" + dateAdded +
                ", order=" + order +
                '}';
    }

    @Override
    public int compareTo(Object another) {
        if (dateAdded.equals(((VidItem) another).dateAdded)) {
            if (order != null && ((VidItem) another).order != null
                    && order < ((VidItem) another).order )
            return -1;
        }
        return (dateAdded > ((VidItem) another).dateAdded ? -1 : 1);
    }

    public static void main(String[] args) {
        VidItem vidItem = new VidItem();
        vidItem.dateAdded = 100l;
        vidItem.order = 1;

        VidItem vidItem2 = new VidItem();
        vidItem2.order = 2;
        vidItem2.dateAdded = 100l;

        VidItem vidItem3 = new VidItem();
        vidItem3.order = 1;
        vidItem3.dateAdded = 102l;

        List<VidItem> vidItems = new ArrayList<VidItem>();
        vidItems.add(vidItem);
        vidItems.add(vidItem2);
        vidItems.add(vidItem3);
        System.out.println("vidItems = " + vidItems);
        Collections.sort(vidItems);
        System.out.println("vidItems = " + vidItems);

    }
}
