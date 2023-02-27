package com.example.chrome1;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;

import com.example.chrome1.activity.Alias1Activity;

public class AppForeBackStatusCallback implements Application.ActivityLifecycleCallbacks {

    /**
     * 活动的Activity数量,为1时，APP处于前台状态，为0时，APP处于后台状态
     */
    private int activityCount = 0;

    /**
     * 最后一次可见的Activity
     * 用于比对Activity，这样可以排除启动应用时的这种特殊情况，
     * 如果启动应用时也需要锁屏等操作，请在启动页里进行操作。
     */
    private Activity lastVisibleActivity;

    /**
     * 最大无需解锁时长 5分钟 单位：毫秒
     */
    private final static long MAX_UNLOCK_DURATION = 5 * 60 * 1000;

    /**
     * 最后一次离开应用时间 单位：毫秒
     */
    private long lastTime;

    @Override

    public void onActivityCreated(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityStarted(Activity activity) {
        activityCount++;
    }

    @Override
    public void onActivityResumed(Activity activity) {
        // 后台进程切换到前台进程,不包含启动应用时的这种特殊情况
        //最后一次可见Activity是被唤醒的Activity && 活动的Activity数量为1
        if (lastVisibleActivity == activity && activityCount == 1) {
            //Background -> Foreground , do something
            startLockScreen(activity);
        }

        lastVisibleActivity = activity;
    }

    /**
     * 打开手势密码
     *
     * @param activity Activity
     */
    private void startLockScreen(Activity activity) {
        if (lockScreen(activity)) {
            //Intent intent = new Intent(activity, Op.class);
            //activity.startActivity(intent);
        }
    }

    /**
     * 锁屏
     *
     * @param activity Activity
     * @return true 锁屏，反之不锁屏
     */
    private boolean lockScreen(Activity activity) {
        //解锁未超时，不锁屏
        if (!unlockTimeout())
            return false;
        //不满足其它条件，不锁屏，#备用#
        if (!otherCondition()) {
            return false;
        }
        //锁屏
        return true;
    }

    /**
     * 由后台切到前台时，解锁时间超时
     *
     * @return 时间间隔大于解锁时长为true，反之为false
     */
    private boolean unlockTimeout() {
        //当前时间和上次离开应用时间间隔
        long dTime = System.currentTimeMillis() - lastTime;
        return dTime > MAX_UNLOCK_DURATION;
    }

    /**
     * 其它条件
     *
     * @return boolean
     */
    private boolean otherCondition() {
        return true;
    }


    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {
        activityCount--;
        lastTime = System.currentTimeMillis();
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }
}