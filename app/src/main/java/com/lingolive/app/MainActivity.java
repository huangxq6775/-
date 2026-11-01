package com.lingolive.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.*;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.*;
import org.json.*;
import java.util.*;

public class MainActivity extends Activity {
    private static final int GREEN = 0xFF216B5A, INK = 0xFF183B33, MUTED = 0xFF71827A;
    private static final String[] NAMES = {"中文", "English", "日本語", "한국어", "Français", "Deutsch", "Español", "हिन्दी"};
    private static final String[] CODES = {"zh", "en", "ja", "ko", "fr", "de", "es", "hi"};
    private static final String[] LOCALES = {"zh-CN", "en-US", "ja-JP", "ko-KR", "fr-FR", "de-DE", "es-ES", "hi-IN"};
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final TranslationRevision revision = new TranslationRevision();
    private final List<Record> records = new ArrayList<>();
    private Spinner sourcePicker, targetPicker;
    private TextView status, sourceText, targetText, levelText, pairLabel;
    private Button mic, speak;
    private LinearLayout historyList;
    private SpeechRecognizer recognizer;
    private Translator translator;
    private TextToSpeech tts;
    private boolean requested, modelReady, downloading, destroyed, ttsReady, swapping;
    private int sourceIndex = 0, targetIndex = 1, pairGeneration, speechGeneration, historyEpoch;
    private String lastPartial = "", latestTranslation = "";
    private Runnable pendingPartial;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        sourceIndex = getPreferences(0).getInt("source", 0);
        targetIndex = getPreferences(0).getInt("target", 1);
        readHistory();
        buildUi();
        tts = new TextToSpeech(this, result -> ttsReady = result == TextToSpeech.SUCCESS);
        resetPair();
    }

    private void buildUi() {
        LinearLayout root = column();
        root.setBackgroundColor(0xFFF6F8F5);
        root.setPadding(dp(22), dp(12), dp(22), dp(12));
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                root.setPadding(dp(22) + bars.left, dp(12) + bars.top, dp(22) + bars.right, dp(12) + bars.bottom);
            } else {
                root.setPadding(dp(22) + insets.getSystemWindowInsetLeft(), dp(12) + insets.getSystemWindowInsetTop(), dp(22) + insets.getSystemWindowInsetRight(), dp(12) + insets.getSystemWindowInsetBottom());
            }
            return Build.VERSION.SDK_INT >= 30 ? WindowInsets.CONSUMED : insets.consumeSystemWindowInsets();
        });
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout content = column();
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
        setContentView(root);
        root.requestApplyInsets();

        LinearLayout brand = row();
        TextView logo = text("声译", 29, INK); logo.setTypeface(null, Typeface.BOLD);
        brand.addView(logo, new LinearLayout.LayoutParams(0, -2, 1));
        TextView badge = text("LINGO LIVE", 11, GREEN);
        badge.setLetterSpacing(.16f); badge.setPadding(dp(12), dp(8), dp(12), dp(8));
        badge.setBackground(shape(0xFFE5EEE7, 18)); brand.addView(badge);
        content.addView(brand);
        TextView intro = text("让每一句话，都被听懂。", 14, MUTED);
        add(content, intro, 8, 24);

        LinearLayout languageCard = row();
        languageCard.setPadding(dp(4), dp(9), dp(4), dp(9));
        languageCard.setBackground(shape(Color.WHITE, 22));
        sourcePicker = picker(sourceIndex); targetPicker = picker(targetIndex);
        sourcePicker.setContentDescription("源语言"); targetPicker.setContentDescription("目标语言");
        languageCard.addView(sourcePicker, new LinearLayout.LayoutParams(0, dp(56), 1));
        Button swap = button("⇄", 0xFFEAF1EB, GREEN);
        swap.setTextSize(26); swap.setContentDescription("互换源语言与目标语言");
        languageCard.addView(swap, new LinearLayout.LayoutParams(dp(48), dp(48)));
        languageCard.addView(targetPicker, new LinearLayout.LayoutParams(0, dp(56), 1));
        add(content, languageCard, 0, 20);
        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (swapping) return;
                int s = sourcePicker.getSelectedItemPosition(), t = targetPicker.getSelectedItemPosition();
                if (s != sourceIndex || t != targetIndex) {
                    sourceIndex = s; targetIndex = t; resetPair();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        };
        sourcePicker.setOnItemSelectedListener(listener); targetPicker.setOnItemSelectedListener(listener);
        swap.setOnClickListener(v -> {
            swapping = true;
            int old = sourceIndex; sourceIndex = targetIndex; targetIndex = old;
            sourcePicker.setSelection(sourceIndex); targetPicker.setSelection(targetIndex);
            swapping = false; resetPair();
        });

        LinearLayout liveCard = column();
        liveCard.setPadding(dp(22), dp(22), dp(22), dp(22));
        liveCard.setBackground(shape(Color.WHITE, 24));
        pairLabel = text("实时语音翻译", 12, GREEN);
        pairLabel.setLetterSpacing(.06f); liveCard.addView(pairLabel);
        sourceText = text("点击下方按钮，开始说话", 23, INK);
        sourceText.setTextIsSelectable(true); sourceText.setMinHeight(dp(90));
        add(liveCard, sourceText, 18, 18);
        View divider = new View(this); divider.setBackgroundColor(0xFFE8EEE8);
        liveCard.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
        TextView translationLabel = text("译文", 12, MUTED); add(liveCard, translationLabel, 18, 10);
        targetText = text("翻译会在这里实时呈现", 25, GREEN);
        targetText.setTextIsSelectable(true); targetText.setMinHeight(dp(90)); liveCard.addView(targetText);
        LinearLayout actions = row();
        Button copy = button("复制译文", 0xFFF0F5F0, GREEN);
        speak = button("朗读译文", 0xFFF0F5F0, GREEN);
        actions.addView(copy, new LinearLayout.LayoutParams(0, dp(46), 1));
        Space gap = new Space(this); actions.addView(gap, new LinearLayout.LayoutParams(dp(10), 1));
        actions.addView(speak, new LinearLayout.LayoutParams(0, dp(46), 1));
        add(liveCard, actions, 20, 0);
        copy.setOnClickListener(v -> {
            if (latestTranslation.isEmpty()) { toast("还没有可复制的译文"); return; }
            ((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("译文", latestTranslation));
            toast("译文已复制");
        });
        speak.setOnClickListener(v -> readAloud());
        content.addView(liveCard);

        levelText = text("·  ·  ·  ·  ·  ·  ·", 24, GREEN);
        levelText.setGravity(Gravity.CENTER); add(content, levelText, 14, 2);
        status = text("准备就绪", 13, MUTED); status.setGravity(Gravity.CENTER);
        add(content, status, 0, 14);
        mic = button("开始实时翻译", GREEN, Color.WHITE); mic.setTextSize(17);
        add(content, mic, 0, 12); mic.getLayoutParams().height = dp(60);
        mic.setOnClickListener(v -> {
            if (requested) { stopSpeech(false); setStatus("已暂停，点击可继续"); }
            else begin();
        });
        TextView note = text("首次翻译需联网下载语言模型。语音识别由手机系统提供，可能需要联网并处理音频。仅已完成的翻译保存在本机，可随时清空。", 11, MUTED);
        note.setGravity(Gravity.CENTER); note.setLineSpacing(dp(3), 1); add(content, note, 0, 26);

        LinearLayout recent = row();
        TextView title = text("最近翻译", 18, INK); title.setTypeface(null, Typeface.BOLD);
        recent.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        Button clear = button("清空", 0xFFF6F8F5, MUTED); recent.addView(clear);
        content.addView(recent);
        clear.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("清空翻译记录？")
            .setMessage("删除保存在本机的所有翻译记录。")
            .setNegativeButton("取消", null).setPositiveButton("清空", (d, w) -> {
                historyEpoch++; records.clear(); saveHistory(); renderHistory();
            }).show());
        historyList = column(); add(content, historyList, 10, 8); renderHistory();
    }

    private void begin() {
        if (sourceIndex == targetIndex) { toast("请选择两种不同的语言"); return; }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            new AlertDialog.Builder(this).setTitle("未找到语音识别服务")
                .setMessage("请在系统设置中启用语音识别服务。部分 Android 手机需先安装兼容的语音服务。")
                .setPositiveButton("知道了", null).show(); return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 10); return;
        }
        tts.stop(); requested = true; updateMic();
        if (modelReady) startSpeech(); else prepareModel();
    }

    private void resetPair() {
        stopSpeech(true); pairGeneration++; revision.next(); modelReady = false; downloading = false;
        if (translator != null) translator.close();
        translator = null; latestTranslation = "";
        if (tts != null) tts.stop();
        sourceText.setText("点击下方按钮，开始说话"); targetText.setText("翻译会在这里实时呈现");
        pairLabel.setText(getString(R.string.language_pair, NAMES[sourceIndex], NAMES[targetIndex]));
        setStatus(sourceIndex == targetIndex ? "请选择两种不同的语言" : "点击开始 · 首次使用会下载语言模型");
        getPreferences(0).edit().putInt("source", sourceIndex).putInt("target", targetIndex).apply();
    }

    private void prepareModel() {
        if (downloading) return;
        int generation = pairGeneration;
        if (translator == null) translator = Translation.getClient(new TranslatorOptions.Builder()
            .setSourceLanguage(CODES[sourceIndex]).setTargetLanguage(CODES[targetIndex]).build());
        downloading = true; setStatus("正在准备语言模型，请保持联网…"); updateMic();
        translator.downloadModelIfNeeded(new DownloadConditions.Builder().build())
            .addOnSuccessListener(v -> {
                if (destroyed || generation != pairGeneration) return;
                downloading = false; modelReady = true; updateMic();
                if (requested) startSpeech(); else setStatus("语言模型已就绪");
            }).addOnFailureListener(error -> {
                if (destroyed || generation != pairGeneration) return;
                downloading = false; requested = false; updateMic();
                setStatus("模型下载失败，请检查网络后重试");
            });
    }

    private void startSpeech() {
        if (!requested || destroyed || !modelReady) return;
        if (recognizer != null) recognizer.destroy();
        int generation = ++speechGeneration;
        lastPartial = "";
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {
            private boolean valid() { return !destroyed && generation == speechGeneration; }
            @Override public void onReadyForSpeech(Bundle params) { if (valid()) setStatus("正在聆听 · 请说" + NAMES[sourceIndex]); }
            @Override public void onBeginningOfSpeech() { if (valid()) setStatus("正在识别，译文会持续更新…"); }
            @Override public void onRmsChanged(float rms) {
                if (!valid()) return;
                int bars = Math.max(1, Math.min(7, (int)((rms + 2) / 2)));
                levelText.setText(meter(bars));
            }
            @Override public void onBufferReceived(byte[] buffer) { }
            @Override public void onEndOfSpeech() { if (valid()) setStatus("正在完成这一句…"); }
            @Override public void onPartialResults(Bundle results) {
                if (!valid() || !requested) return;
                String value = first(results);
                if (value.isEmpty() || value.equals(lastPartial)) return;
                lastPartial = value; sourceText.setText(value);
                revision.next();
                if (pendingPartial == null) {
                    pendingPartial = () -> {
                        pendingPartial = null;
                        translate(lastPartial, false, revision.next());
                    };
                    handler.postDelayed(pendingPartial, 250);
                }
            }
            @Override public void onResults(Bundle results) {
                if (!valid()) return;
                if (pendingPartial != null) { handler.removeCallbacks(pendingPartial); pendingPartial = null; }
                String value = first(results);
                if (value.isEmpty()) value = lastPartial;
                if (!value.isEmpty()) {
                    sourceText.setText(value); translate(value, true, revision.next());
                }
                levelText.setText("·  ·  ·  ·  ·  ·  ·");
                if (requested) scheduleRestart(generation, 400);
                else setStatus("已暂停，点击可继续");
            }
            @Override public void onError(int error) {
                if (!valid()) return;
                if (!requested) { setStatus("已暂停，点击可继续"); return; }
                if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    setStatus("没有听清，继续聆听…"); scheduleRestart(generation, 700); return;
                }
                requested = false; updateMic();
                String message;
                switch (error) {
                    case SpeechRecognizer.ERROR_NETWORK:
                    case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: message = "语音服务网络异常，请检查网络后重试"; break;
                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: message = "请在系统设置中允许麦克风权限"; break;
                    case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: message = "语音服务正忙，请稍后重试"; break;
                    case SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED:
                    case SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE: message = "系统语音服务暂不支持此语言，请下载对应语言或切换服务"; break;
                    case SpeechRecognizer.ERROR_TOO_MANY_REQUESTS: message = "语音请求过于频繁，请稍后重试"; break;
                    default: message = "语音服务暂不可用（" + error + "），点击重试";
                }
                setStatus(message);
            }
            @Override public void onEvent(int eventType, Bundle params) { }
        });
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, LOCALES[sourceIndex]);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        try { recognizer.startListening(intent); }
        catch (RuntimeException e) {
            requested = false; updateMic(); setStatus("无法启动语音服务，请检查系统设置");
        }
    }

    private void scheduleRestart(int generation, int delay) {
        handler.postDelayed(() -> { if (requested && generation == speechGeneration && !destroyed) startSpeech(); }, delay);
    }

    private void stopSpeech(boolean cancel) {
        requested = false;
        if (pendingPartial != null) { handler.removeCallbacks(pendingPartial); pendingPartial = null; }
        if (cancel) speechGeneration++;
        if (recognizer != null) {
            if (cancel) { recognizer.cancel(); recognizer.destroy(); recognizer = null; }
            else recognizer.stopListening();
        }
        if (mic != null) updateMic();
        if (levelText != null) levelText.setText("·  ·  ·  ·  ·  ·  ·");
    }

    private void translate(String value, boolean complete, long ticket) {
        if (translator == null || !modelReady) return;
        int generation = pairGeneration, epoch = historyEpoch;
        String from = NAMES[sourceIndex], to = NAMES[targetIndex];
        translator.translate(value).addOnSuccessListener(result -> {
            if (destroyed || generation != pairGeneration) return;
            if (revision.isCurrent(ticket)) { latestTranslation = result; targetText.setText(result); }
            if (complete && epoch == historyEpoch) {
                records.add(0, new Record(value, result, from + " → " + to));
                while (records.size() > 30) records.remove(records.size() - 1);
                saveHistory(); renderHistory();
            }
        }).addOnFailureListener(error -> {
            if (!destroyed && generation == pairGeneration && revision.isCurrent(ticket)) {
                latestTranslation = ""; targetText.setText("翻译失败，请重新说一次");
            }
        });
    }

    private void readAloud() {
        if (latestTranslation.isEmpty()) { toast("还没有可朗读的译文"); return; }
        if (!ttsReady) { toast("系统朗读引擎尚未就绪"); return; }
        stopSpeech(true);
        int result = tts.setLanguage(Locale.forLanguageTag(LOCALES[targetIndex]));
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            setStatus("系统缺少该语言的语音包，请在系统设置中安装"); return;
        }
        int queued = tts.speak(latestTranslation, TextToSpeech.QUEUE_FLUSH, null, "translation");
        setStatus(queued == TextToSpeech.SUCCESS ? "已暂停麦克风，正在朗读译文" : "朗读失败，请检查系统语音引擎");
    }

    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        if (code != 10) return;
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) begin();
        else setStatus("需要麦克风权限；可在系统设置 → 应用 → 声译中开启");
    }
    @Override protected void onPause() {
        super.onPause(); stopSpeech(true);
        if (tts != null) tts.stop();
        setStatus("已暂停，点击可继续");
    }
    @Override protected void onDestroy() {
        destroyed = true; stopSpeech(true); handler.removeCallbacksAndMessages(null);
        if (translator != null) translator.close();
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }

    private static String first(Bundle bundle) {
        ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        return matches == null || matches.isEmpty() ? "" : matches.get(0).trim();
    }
    private String meter(int bars) {
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < 7; i++) value.append(i < bars ? "▏" : "·");
        return value.toString();
    }
    private void updateMic() {
        mic.setText(requested ? (downloading ? "取消准备" : "停止聆听") : "开始实时翻译");
        mic.setBackground(shape(requested ? 0xFFA04332 : GREEN, 20));
    }
    private void setStatus(String value) { if (status != null) status.setText(value); }
    private void toast(String value) { Toast.makeText(this, value, Toast.LENGTH_SHORT).show(); }
    private int dp(float value) { return (int)(value * getResources().getDisplayMetrics().density + .5f); }
    private LinearLayout column() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.VERTICAL); return v; }
    private LinearLayout row() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.HORIZONTAL); v.setGravity(Gravity.CENTER_VERTICAL); return v; }
    private TextView text(String value, int size, int color) {
        TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color); v.setLineSpacing(dp(3), 1); return v;
    }
    private Button button(String value, int background, int foreground) {
        Button v = new Button(this); v.setText(value); v.setTextColor(foreground); v.setTextSize(13);
        v.setAllCaps(false); v.setMinWidth(0); v.setMinimumWidth(0); v.setMinHeight(0); v.setMinimumHeight(dp(44));
        v.setPadding(dp(14), dp(8), dp(14), dp(8)); v.setStateListAnimator(null); v.setBackground(shape(background, 16)); return v;
    }
    private GradientDrawable shape(int color, int radius) {
        GradientDrawable v = new GradientDrawable(); v.setColor(color); v.setCornerRadius(dp(radius)); return v;
    }
    private void add(LinearLayout parent, View view, int top, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.setMargins(0, dp(top), 0, dp(bottom)); parent.addView(view, p);
    }
    private Spinner picker(int selected) {
        Spinner picker = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, NAMES);
        picker.setAdapter(adapter); picker.setSelection(selected); return picker;
    }
    private void renderHistory() {
        if (historyList == null) return;
        historyList.removeAllViews();
        if (records.isEmpty()) {
            TextView empty = text("还没有翻译记录\n开始一段跨越语言的对话吧", 13, MUTED);
            empty.setGravity(Gravity.CENTER); empty.setPadding(0, dp(20), 0, dp(20)); historyList.addView(empty); return;
        }
        for (Record record : records) {
            LinearLayout item = column(); item.setBackground(shape(Color.WHITE, 18)); item.setPadding(dp(18), dp(16), dp(18), dp(16));
            item.addView(text(record.pair, 11, MUTED));
            TextView original = text(record.source, 15, INK); original.setTextIsSelectable(true); add(item, original, 8, 6);
            TextView translated = text(record.target, 17, GREEN); translated.setTextIsSelectable(true); item.addView(translated);
            add(historyList, item, 0, 10);
        }
    }
    private void readHistory() {
        try {
            JSONArray data = new JSONArray(getPreferences(0).getString("history", "[]"));
            for (int i = 0; i < Math.min(data.length(), 30); i++) {
                JSONObject v = data.getJSONObject(i); records.add(new Record(v.getString("source"), v.getString("target"), v.getString("pair")));
            }
        } catch (JSONException ignored) { records.clear(); }
    }
    private void saveHistory() {
        JSONArray data = new JSONArray();
        for (Record record : records) {
            JSONObject v = new JSONObject();
            try { v.put("source", record.source); v.put("target", record.target); v.put("pair", record.pair); data.put(v); }
            catch (JSONException ignored) { }
        }
        getPreferences(0).edit().putString("history", data.toString()).apply();
    }
    private static class Record {
        final String source, target, pair;
        Record(String source, String target, String pair) { this.source = source; this.target = target; this.pair = pair; }
    }
}
