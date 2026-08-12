package com.musicdownloader.musicfreeapp825v2.logic.utils.online;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

/**
 * Created by Quang Phúc on 12/8/26
 */
public interface SearchCallback {
    void onSuccess(@NonNull ArrayList<VideoEntity> var1, @Nullable Object var2);

    void onError(Exception var1);

    void onRecordException(@NonNull Exception var1);
}
