package com.ndzl.zph;

import androidx.core.content.FileProvider;

//https://developer.android.com/reference/androidx/core/content/FileProvider
//https://developer.android.com/training/secure-file-sharing/setup-sharing

public class OriginatingAppFileProvider extends FileProvider {
    public OriginatingAppFileProvider() {
        super(R.xml.provider_paths);
    }


}
