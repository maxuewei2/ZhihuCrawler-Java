package zhihucrawler;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;



class Request {
    private final CookieProvider cookieProvider;
    private final ProxyProvider proxyProvider;
    private final int maxTryNum;
    private HttpClient httpClient;
    private Proxy proxy;
    private Instant last = Instant.now();

    Request(CookieProvider cookieProvider, ProxyProvider proxyProvider, int maxTryNum) {
        this.cookieProvider = cookieProvider;
        this.proxyProvider = proxyProvider;
        this.maxTryNum = maxTryNum;
        if (proxyProvider != null) {
            setProxy(proxyProvider.getProxy());
        } else {
            setProxy(null);
        }
    }

    Proxy getProxy() {
        return proxy;
    }

    void setProxy(Proxy proxy) {
        this.proxy = proxy;
        HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2);
        if (proxy != null) {
            clientBuilder.proxy(ProxySelector.of(new InetSocketAddress(proxy.getIP(), proxy.getPort())));
        }
        httpClient = clientBuilder.build();
    }

    boolean changeProxy() {
        last = Instant.now();
        if (proxyProvider == null) {
            return false;
        }
        Proxy tmp = proxyProvider.getProxy();
        if (tmp != proxy) {
            setProxy(tmp);
            return true;
        }
        return false;
    }

    private HttpResponse<String> sendGet(String url, String referer) throws IOException, InterruptedException {
        String userAgent = "Mozilla/5.0 (iPhone; U; CPU iPhone OS 11_3_2 like Mac OS X; en-US) AppleWebKit/602.1.7 (KHTML, like Gecko) Version/11.1.1 Mobile/8F2 Safari/6533.18.5";
        Cookie cookie = null;
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .GET()
                .timeout(Duration.ofSeconds(5))
                .uri(URI.create(url))
                .setHeader("accept", "*/*")
                .setHeader("accept-language", "zh-CN,zh;q=0.9")
                //.setHeader("accept-encoding", "gzip")
                .setHeader("cache-control", "no-cache")
                .setHeader("dnt", "1")
                .setHeader("pragma", "no-cache")
                .setHeader("referer", referer)
                .setHeader("sec-fetch-mode", "cors")
                .setHeader("sec-fetch-site", "same-origin")
                //.setHeader("x-ab-param", "qap_ques_invite=0;zr_slotpaidexp=2;se_topicfeed=0;tp_club_header=1;tp_club_android_join=0;top_ebook=0;zr_art_rec=base;zr_km_sku_thres=false;se_pek_test=1;li_purchase_test=0;li_salt_hot=1;se_senet=0;se_search_feed=N;tp_topic_entry=0;soc_bigone=1;soc_zcfw_broadcast2=1;soc_leave_recommend=2;pf_foltopic_usernum=50;li_query_match=0;zr_km_feed_nlp=old;soc_special=0;li_cln_vl=no;zr_video_rank=new_rank;se_p_slideshow=1;se_site_onebox=0;se_wannasearch=a;tp_qa_metacard_top=top;zw_sameq_sorce=999;zr_se_new_xgb=0;se_zu_onebox=0;se_mobileweb=1;zr_answer_rec_cp=open;zr_video_recall=current_recall;se_lottery=0;se_expired_ob=0;tp_club_feed=0;top_quality=0;top_v_album=1;zr_km_answer=open_cvr;se_preset_label=1;se_sug_entrance=0;se_college=default;soc_cardheight=0;tsp_vote=2;se_webmajorob=0;se_featured=1;zr_ans_rec=gbrank;se_likebutton=0;se_cp2=0;se_cardrank_4=1;se_multi_task_new=2;soc_zcfw_shipinshiti=0;ls_fmp4=0;li_se_across=1;zr_rec_answer_cp=close;se_dnn_unbias=1;se_ad_index=10;li_qc_pt=0;zr_km_slot_style=event_card;se_sug=1;soc_zuichangfangwen=0;se_new_topic=0;se_billboardsearch=0;tp_sft_v2=d;soc_wonderuser_recom=2;li_tjys_ec_ab=0;soc_update=1;li_android_vip=0;li_qa_cover=old;zw_payc_qaedit=0;zr_rel_search=base;se_hotsearch=0;li_ebook_audio=0;se_club_post=5;tp_club_tab=0;tp_qa_toast=1;tp_qa_metacard=1;pf_newguide_vertical=0;tp_score_1=a;li_hot_score_ab=0;li_vip_lr=1;zr_km_style=base;se_websearch=3;tp_topic_style=0;li_se_media_icon=1;li_sc=no;zr_intervene=0;se_movietab=1;se_entity_model=1;se_hotmore=2;tp_club_join=0;soc_stickypush=0;ug_zero_follow_0=0;ls_zvideo_rec=2;soc_ri_merge=0;top_ydyq=X;li_vip_no_ad_mon=0;li_sku_bottom_bar_re=0;se_waterfall=0;se_subtext=1;qap_payc_invite=0;tp_meta_card=0;qap_question_author=0;se_amovietab=1;tp_topic_rec=1;soc_authormore=2;li_de=no;zr_paid_answer_exp=0;zr_search_satisfied=1;se_webrs=1;se_famous=1;li_qa_btn_text=0;se_colorfultab=1;se_perf=0;se_zu_recommend=0;tp_sft=a;li_pay_banner_type=6;zr_article_new=close;zr_test_aa1=0;top_hotcommerce=1;pf_creator_card=1;se_ios_spb309=1;soc_zcfw_badcase=0;se_use_zitem=0;top_new_feed=5;se_spb309=0;tp_topic_head=0;zr_video_rank_nn=new_rank;tp_club_qa=1;se_ab=0;se_related_index=3;se_ctx_rerank=0;se_member_rescore=1;top_root=0;pf_fuceng=1;zr_km_sku_mix=sku_55;top_test_4_liguangyi=1;ug_follow_answerer=0;ug_follow_answerer_0=0;ug_follow_topic_1=2;li_answer_card=0;pf_noti_entry_num=0;ug_zero_follow=0;tp_club_qa_pic=1;qap_question_visitor= 0;se_entity_model_14=0;se_col_boost=1;se_whitelist=1;se_cardrank_3=0;se_new_merger=1;se_timebox_up=0;tp_sticky_android=2;soc_brdcst3=0;se_pek_test2=1;se_adxtest=1;ls_videoad=2;sem_up_growth=in_app;li_album_liutongab=0;li_paid_answer_exp=0;zr_des_detail=0;se_cardrank_1=0;se_hot_timebox=1;se_multianswer=0;tp_header_style=1;soc_bignew=1;ug_goodcomment_0=1;se_preset_tech=0;soc_notification=1;soc_yxzl_zcfw=0;se_backsearch=1;tsp_hotlist_ui=1;zr_expslotpaid=1;se_agency= 0;li_se_section=1;tp_m_intro_re_topic=1;li_se_heat=1;se_pek_test3=1;se_aa_base=1;se_topiclabel=1;se_payconsult=5;tp_topic_tab=0;tsp_redirecthotlist=5;se_college_cm=1;ls_zvideo_license=1;li_qa_new_cover=1;se_ltr_dnn_cp=0;tp_club_pk=1;se_webtimebox=1;se_ltr_cp_new=0;ug_fw_answ_aut_1=0;ug_newtag=1;zr_slot_cold_start=aver;se_auto_syn=0;se_time_threshold=0;se_rel_search=1;se_bert_v2=0;soc_newfeed=0;li_video_section=1;se_cardrank_2=1;soc_zcfw_broadcast=0;top_universalebook=1;zr_slot_training=1")
                .setHeader("x-requested-with", "fetch");
        if (cookieProvider != null) {
            cookie = cookieProvider.getCookie();
            if (cookie != null) {
                builder.setHeader("cookie", cookie.getCookieString());
                userAgent = cookie.getUserAgent();
            }
        }
        util.logInfo(Thread.currentThread().getName() + " using " + cookie + " " + proxy + " GET " + url);
        builder.setHeader("user-agent", userAgent);
        HttpRequest request = builder.build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendRequest(String url, String referer) throws InterruptedException, IOException {
        Throwable throwable = null;
        for (int i = 0; i < maxTryNum; i++) {
            try {
                return sendGet(url, referer);
            } catch (IOException e) {
                throwable = e;
            }
        }
        throw new IOException(throwable);
    }

    HttpResponse<String> request(String url, String referer, Proxy pro) throws InterruptedException, IOException {
        try {
            return sendRequest(url, referer);
        } catch (IOException throwable) {
            String msg = throwable.getMessage();
            /*if(throwable instanceof HttpConnectTimeoutException){
                util.logWarning("HTTPException ConnectTimeOut "+pro+" "+msg);
            }else if(throwable instanceof HttpTimeoutException){
                util.logWarning("HTTPException TimeOut "+pro+" "+msg);
            }else if(throwable instanceof ConnectException){
                util.logWarning("HTTPException  "+pro+" "+msg);
            }*/
            util.logWarning("Request " + pro + " " + msg);
            if (pro == null) {
                throw new IOException("Network Error", throwable);
            }
            pro.setError();
            throw new IOException("Proxy Network Error", throwable);
        }
    }

    HttpResponse<String> request(String url, String referer) throws InterruptedException, IOException {
        //间隔一段时间换一下代理
        if (Duration.between(last, Instant.now()).toSeconds() > 600) {
            changeProxy();
        }
        //proxy为null时表示未使用代理
        //尽量使用代理
        if (proxy == null) {
            changeProxy();
        }
        return request(url, referer, proxy);
    }

}
