package com.dirror.music.ui.activity

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.animation.LinearInterpolator
import android.widget.SeekBar
import com.dirror.music.MyApplication
import com.dirror.music.R
import com.dirror.music.cloudmusic.SongInnerData
import com.dirror.music.service.MusicService
import com.dirror.music.ui.base.BaseActivity
import com.dirror.music.util.*
import kotlinx.android.synthetic.main.activity_play.*

class PlayActivity : BaseActivity(), SeekBar.OnSeekBarChangeListener {

    private lateinit var musicBroadcastReceiver: MusicBroadcastReceiver // 音乐广播接收
    var duration = 0
    private val handler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg?.what) {
                MSG_PROGRESS -> updateProgress()
            }
        }
    }
    val MSG_PROGRESS = 0

    override fun getLayoutId(): Int {
        return R.layout.activity_play
    }

    override fun initData() {
        val intentFilter = IntentFilter() // Intent 过滤器
        intentFilter.addAction("com.dirror.music.MUSIC_BROADCAST") // 只接收 "com.dirror.foyou.MUSIC_BROADCAST" 标识广播
        musicBroadcastReceiver = MusicBroadcastReceiver() //
        registerReceiver(musicBroadcastReceiver, intentFilter) // 注册接收器
    }

    override fun initView() {
        // 获取现在歌曲信息
        getNowSongData()


        updateProgress()


        ivPlay.setOnClickListener {
            // 更新
            MyApplication.musicBinderInterface?.changePlayState()
            refreshPlayState()
        }

        // 下一曲
        ivNext.setOnClickListener {
            MyApplication.musicBinderInterface?.playNext()
        }

        // 上一曲
        ivLast.setOnClickListener {
            MyApplication.musicBinderInterface?.playLast()
        }

        // 切换播放模式
        ivMode.setOnClickListener {
            MyApplication.musicBinderInterface?.changePlayMode()
        }

        // 点击 titleBar，关闭 Activity
        titleBar.setOnClickListener {
            finish()
        }

        // 点击评论，跳转
        ivComment.setOnClickListener {
            val intent = Intent(this, CommentActivity::class.java)
            intent.putExtra("long_music_id", song?.id)
            startActivity(intent)
            overridePendingTransition(
                R.anim.anim_slide_enter_bottom,
                R.anim.anim_no_anim
            )
        }

        // 进度条变化的监听
        seekBar.setOnSeekBarChangeListener(this)

    }

    override fun onStart() {
        super.onStart()
        //
    }

    override fun onResume() {
        super.onResume()
        refreshPlayState()
    }

    private fun refreshPlayState() {
        if (MyApplication.musicBinderInterface?.getPlayState()!!) {
            // 播放
            ivPlay.setImageResource(R.drawable.ic_pause_btn)
            startRotateAlways()
            // 开启进度更新
            handler.sendEmptyMessage(MSG_PROGRESS)
        } else {
            // 暂停
            ivPlay.setImageResource(R.drawable.ic_play_btn)
            pauseRotateAlways()
            // 停止更新进度
            handler.removeMessages(MSG_PROGRESS)
        }
    }

    private var rotation = 0f
    private fun pauseRotateAlways() {
        rotation = ivCover.rotation
        loge("rotation:$rotation")
        objectAnimator.pause()
    }

    private val objectAnimator: ObjectAnimator by lazy {
        ObjectAnimator.ofFloat(ivCover, "rotation", rotation, rotation + 360f).apply {
            interpolator = LinearInterpolator()
            duration = 25000
            repeatCount = -1
            start()
        }
    }


    private fun startRotateAlways() {
        objectAnimator.resume()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 取消广播接收器的注册
        unregisterReceiver(musicBroadcastReceiver)
        // 清空 Handler 发送的所有消息，防止内存泄漏
        handler.removeCallbacksAndMessages(null)
    }

    inner class MusicBroadcastReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            getNowSongData()

            // 根据当前播放，修改图标
            val mode = MyApplication.musicBinderInterface?.getPlayMode()
            if (mode != null) {
                when (mode) {
                    MusicService.MODE_CIRCLE -> ivMode.setImageResource(R.drawable.ic_bq_player_mode_circle)
                    MusicService.MODE_REPEAT_ONE -> ivMode.setImageResource(R.drawable.ic_bq_player_mode_repeat_one)
                    MusicService.MODE_RANDOM -> ivMode.setImageResource(R.drawable.ic_bq_player_mode_random)
                }
            }
        }
    }

    /**
     * 更新进度
     */
    private fun updateProgress() {
        // 获取当前进度
        val progress = MyApplication.musicBinderInterface?.getProgress()?:0
        duration = MyApplication.musicBinderInterface?.getDuration()?:0
        // 设置进度条最大值
        seekBar.max = duration
        // 更新进度
        seekBar.progress = progress
        tvProgress.text = TimeUtil.parseDuration(progress)
        tvDuration.text = TimeUtil.parseDuration(duration)
        // 定时获取进度
        handler.sendEmptyMessageDelayed(MSG_PROGRESS,1000)
    }

    /**
     * 进度改变的回调
     * @param p1 改变之后的进度
     * @param p2 true 通过用户手指拖动 false 通过代码方式改变进度
     */
    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
        // 判断是否为用户
        if (!p2) return
        // Log.e("手指拖到了：", p1.toString())
        MyApplication.musicBinderInterface?.setProgress(p1)
        updateProgress()
    }

    // 手指触摸
    override fun onStartTrackingTouch(p0: SeekBar?) {

    }

    // 手指离开
    override fun onStopTrackingTouch(p0: SeekBar?) {

    }

    private var song: SongInnerData? = null
    private fun getNowSongData() {
        song = MyApplication.musicBinderInterface?.getNowSongData()?.songs?.get(0)
        if (song != null) {
            val url = song!!.al.picUrl
            GlideUtil.load(url, ivCover)

            tvName.text = song!!.name
            tvArtist.text = parseArtist(song!!.ar)
        }


    }
}