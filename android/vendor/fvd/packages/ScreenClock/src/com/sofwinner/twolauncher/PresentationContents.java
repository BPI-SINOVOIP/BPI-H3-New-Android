package com.sofwinner.twolauncher;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

/**
 * Information about the content we want to show in a presentation.
 */
public class PresentationContents implements Parcelable {
    final int photo;
    final int[] colors;

    public static final Creator<PresentationContents> CREATOR =
            new Creator<PresentationContents>() {
        @Override
        public PresentationContents createFromParcel(Parcel in) {
            return new PresentationContents(in);
        }

        @Override
        public PresentationContents[] newArray(int size) {
            return new PresentationContents[size];
        }
    };

    public PresentationContents(int photo) {
        this.photo = photo;
        colors = new int[] {
                ((int) (Math.random() * Integer.MAX_VALUE)) | 0xFF000000,
                ((int) (Math.random() * Integer.MAX_VALUE)) | 0xFF000000 };
    }

    private PresentationContents(Parcel in) {
        photo = in.readInt();
        colors = new int[] { in.readInt(), in.readInt() };
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(photo);
        dest.writeInt(colors[0]);
        dest.writeInt(colors[1]);
    }
}
