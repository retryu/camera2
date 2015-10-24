package com.mogujie.transformer.camera;

/**
 * Created by retryu on 15/10/14.
 */
public interface CameraFragemntCallBack {
    /**
     * 更新右上角选中计数器
     * @param count
     */
    public void updateIndicatorCount(int count);

    public void finishCamer();
}
