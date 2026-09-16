package com.example

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.example.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificationWidgetRTLTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testNotificationWidgetLayoutIsRTL() {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.widget_quick_voice_notification, null)

        assertNotNull("ویجت اعلان باید با موفقیت اینفلیت شود", view)

        val titleView = view.findViewById<TextView>(R.id.tv_notification_title)
        assertNotNull(titleView)
        assertTrue(titleView.text.toString().contains("کیسه"))

        val subtitleView = view.findViewById<TextView>(R.id.tv_notification_subtitle)
        assertNotNull(subtitleView)
        assertTrue(subtitleView.text.toString().contains("ثبت سریع"))

        val buttonView = view.findViewById<TextView>(R.id.btn_record_action)
        assertNotNull(buttonView)
        assertTrue(buttonView.text.toString().contains("ثبت صوتی"))
    }
}
