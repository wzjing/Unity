package com.wzjing.unity


import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import cn.jiguang.verifysdk.api.JVerificationInterface
import cn.jiguang.verifysdk.api.VerifyListener
import cn.jiguang.verifysdk.api.VerifySDK
import cn.jiguang.verifysdk.api.VerifySDK.CODE_VERIFY_EXCEPTION
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject


class VerificationActivity : AppCompatActivity(), View.OnClickListener, VerifyListener {

    private val TAG = "VerifyActivity"
    private var mProgressbar: LinearLayout? = null
    private val mView: View? = null
    private var mViewVerificationCode: View? = null
    private var mTilNum: EditText? = null
    private var mRlLogin: RelativeLayout? = null
    private var mBtnVerificationCode: Button? = null
    private var mBtnLogin: Button? = null
    private var mLayoutBack: LinearLayout? = null
    private val telRegex = "^((13[0-9])|(15[0-9])|(18[0-9])|(17[0-9])|(19[0-9])|(147,145))\\d{8}$"
    private var tvTip: TextView? = null
    private var mViewMsgWarning: LinearLayout? = null
    private var tvErrorMsg: TextView? = null
    private var mTvNetWarning: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verification)
        initView()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        val intent = intent
        if (intent != null) {
            val action = intent.getIntExtra(Constants.KEY_ACTION, -1)
            val errorMsg = intent.getStringExtra(Constants.KEY_ERORRO_MSG)
            val errorCode = intent.getIntExtra(Constants.KEY_ERROR_CODE, -1)
            if (action == Constants.ACTION_LOGIN_FAILED) {
                if (errorCode == Constants.CODE_LOGIN_CANCELD) {
                    tvErrorMsg!!.text = "一键登录取消，可用短信验证码补充"
                } else {
                    tvErrorMsg!!.text = "一键登录失败，可用短信验证码补充"
                }
                showErrorTheme()
                showErrorMsg(errorMsg)
            }
        }

        ScreenUtils.tryFullScreenWhenLandscape(this)

    }

    private fun initView() {
        mLayoutBack = findViewById<LinearLayout>(R.id.layout_back)
        Log.d(TAG, "layout back:" + mLayoutBack!!)
        mLayoutBack!!.setOnClickListener(this)
        mProgressbar = findViewById<LinearLayout>(R.id.progressbar)
        tvTip = findViewById<TextView>(R.id.tvtip)

        mTilNum = findViewById<EditText>(R.id.til_num)
        mBtnLogin = findViewById<Button>(R.id.btn_login)

        mRlLogin = findViewById<RelativeLayout>(R.id.rl_login)
        mViewVerificationCode = findViewById(R.id.view_verification_code)
        mBtnVerificationCode = findViewById<Button>(R.id.btn_verification_code)


        mViewMsgWarning = findViewById<LinearLayout>(R.id.view_msg_warning)
        tvErrorMsg = findViewById<TextView>(R.id.tv_errormsg)
        mTvNetWarning = findViewById<TextView>(R.id.tv_net_warning)


        mBtnLogin!!.setOnClickListener(this)
        setButtonEnable(true)
    }

    override fun onClick(v: View) {
        if (v.getId() === R.id.layout_back) {
            this.finish()
        } else if (v.getId() === R.id.btn_login) {
            login()
        }
    }

    private fun checkNum(): Boolean {
        val numStr = mTilNum!!.text.toString()
        if (TextUtils.isEmpty(numStr)) {
            ToastUtil.showShortToast(this, "输入号码不能为空")
            return false
        }
        return !TextUtils.isEmpty(numStr) && numStr.matches(telRegex.toRegex())
    }

    private fun login() {
        if (!checkNum()) {//号码格式错误不进行下一步
            tvTip!!.visibility = View.VISIBLE
            return
        }
        tvTip!!.visibility = View.INVISIBLE
        mProgressbar!!.visibility = View.VISIBLE
        mBtnLogin!!.setEnabled(false)
        Log.e(TAG, "phone=" + mTilNum!!.text.toString())
        //        JVerificationInterface.verifyNumber(this, null, mTilNum.getText().toString(), this);
        JVerificationInterface.getToken(
            this
        ) { code, content, operator ->
            if (code == 2000) {
                Thread(Runnable {
                    realVerifyNumber(
                        content,
                        mTilNum!!.text.toString(),
                        operator,
                        this@VerificationActivity
                    )
                }).start()

            } else {
                this@VerificationActivity.onResult(code, content, operator)
            }
        }
    }

    fun showErrorTheme() {
        mViewVerificationCode!!.setVisibility(View.VISIBLE)
        mRlLogin!!.visibility = View.GONE
        mBtnVerificationCode!!.setText("确认")
    }

    fun setButtonEnable(isEnable: Boolean) {
        mBtnLogin!!.setEnabled(isEnable)
        mBtnVerificationCode!!.setEnabled(isEnable)
    }

    override fun onResult(code: Int, token: String, operator: String) {
        Log.e(TAG, "onResult: code=$code,token=$token,operator=$operator")
        val errorMsg = "operator=$operator,code=$code\ncontent=$token"
        runOnUiThread {
            mProgressbar!!.visibility = View.GONE
            mBtnLogin!!.setEnabled(true)
            if (code == Constants.VERIFY_CONSISTENT) {
                toSuccessActivity(Constants.ACTION_VERIFY_SUCCESS)
                Log.e(TAG, "onResult: loginSuccess")
            } else {
                Log.e(TAG, "onResult: loginError")
                toFailedActivigy(code, token)
            }
        }
    }


    private fun toSuccessActivity(action: Int) {
        val intent = Intent(this, LoginResultActivity::class.java)
        intent.putExtra(Constants.KEY_ACTION, action)
        startActivity(intent)
        finish()
    }

    private fun toFailedActivigy(code: Int, errorMsg: String) {
        var msg = errorMsg
        if (code == 9001) {
            msg = "手机号验证不一致"
        } else if (code == 2003) {
            msg = "网络连接不通"
        } else if (code == 2005) {
            msg = "请求超时"
        } else if (code == 2016) {
            msg = "当前网络环境不支持认证"
        } else if (code == 2010) {
            msg = "未开启读取手机状态权限"
        }
        val intent = Intent(this, LoginResultActivity::class.java)
        intent.putExtra(Constants.KEY_ACTION, Constants.ACTION_VERIFY_FAILED)
        intent.putExtra(Constants.KEY_ERORRO_MSG, msg)
        intent.putExtra(Constants.KEY_ERROR_CODE, code)
        startActivity(intent)
        finish()
    }


    private fun showErrorMsg(errorMsg: String?) {
        if (Constants.DEBUG_ENABLE) {
            tvErrorMsg!!.text = errorMsg
        } else {
            tvErrorMsg!!.visibility = View.GONE
        }
        mViewMsgWarning!!.visibility = View.VISIBLE
        mTvNetWarning!!.visibility = View.GONE
    }


    val JSON = MediaType.parse("application/json; charset=utf-8")

    private fun realVerifyNumber(
        token: String,
        phone: String,
        operator: String,
        listener: VerifyListener
    ) {
        var code: Int
        val responseData: String?
        var content: String? = null
        try {
            val client = OkHttpClient()
            val postJson = JSONObject()
            postJson.put("phone", phone)
            postJson.put("token", token)
            val body = postJson.toString()
            val requestBody = RequestBody.create(JSON, body)
            Log.d(TAG, "request url:" + Constants.consistUrl)
            val request = Request.Builder().url(Constants.consistUrl).post(requestBody).build()
            val response = client.newCall(request).execute()
            responseData = response.body()?.string()
            code = response.code()

            Log.d(TAG, "response code = $code ，msg = $responseData")
            if (code != 200) {
                code = VerifySDK.CODE_NET_EXCEPTION
                content = "net error"
            } else if (responseData != null) {
                Log.d(TAG, "verify number, code=$code content=$responseData")
                val resultJson = JSONObject(responseData)
                code = resultJson.optInt("consistencyCode")
                if (code == 9000) {
                    content = "verify consistent"
                } else if (code == 9001) {
                    content = "verify not consistent"
                } else if (code == 9002) {
                    content = "unknown result"
                } else if (code == 9003) {
                    content = "token expired or not exist"
                } else if (code == 9004) {
                    content = "config not found"
                } else if (code == 9005) {
                    content = "verify interval is less than the minimum limit"
                } else if (code == 9006) {
                    content = "frequency of verifying single number is beyond the maximum limit"
                } else if (code == 9007) {
                    content = "beyond daily frequency limit"
                } else if (code == 9010) {
                    content = "miss auth"
                } else if (code == 9011) {
                    content = "auth failed"
                } else if (code == 9012) {
                    content = "parameter invalid"
                } else if (code == 9013) {
                    content = "request method not supported"
                } else if (code == 9015) {
                    content = "http media type not supported"
                } else if (code == 9018) {
                    content = "appKey no money"
                } else if (code == 9031) {
                    content = "not validate user"
                } else if (code == 9099) {
                    content = "bad server"
                }
            } else {
                code = CODE_VERIFY_EXCEPTION
                content = "http error, can't get response"
            }

        } catch (e: Throwable) {
            Log.w(TAG, "phone validate e:$e")
            code = CODE_VERIFY_EXCEPTION
            content = e.toString()
        }

        listener.onResult(code, content, operator)
    }
}
