/*
 * Copyright (c) 2015 South Silicon Valley Microelectronics Inc.
 * Copyright (c) 2015 iComm Corporation
 *
 * This program is free software: you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License 
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

#include <linux/module.h>
#include <linux/platform_device.h>
#include <linux/nl80211.h>
#include <linux/kthread.h>
#include <linux/etherdevice.h>
#include <ssv6200.h>
#include <hci/hctrl.h>
#include <ssv_version.h>
#include "dev_tbl.h"
#include "dev.h"
#include "lib.h"
#include "ssv_rc.h"
#include "ap.h"
#include "efuse.h"
#ifdef CONFIG_SSV_SUPPORT_ANDROID
#include "ssv_pm.h"
#endif
#if LINUX_VERSION_CODE < KERNEL_VERSION(3, 0, 0)
#include "linux_2_6_35.h"
#endif
#ifdef CONFIG_SSV6XXX_DEBUGFS
#include "ssv6xxx_debugfs.h"
#endif
#ifdef MULTI_THREAD_ENCRYPT
#include <linux/cpu.h>
#include <linux/notifier.h>
#endif
MODULE_AUTHOR("iComm Semiconductor Co., Ltd");
MODULE_DESCRIPTION("Support for SSV6xxx wireless LAN cards.");
MODULE_SUPPORTED_DEVICE("SSV6xxx 802.11n WLAN cards");
MODULE_LICENSE("Dual BSD/GPL");
static const struct ieee80211_iface_limit ssv6xxx_p2p_limits[] = {
	{ .max = 2, .types = BIT(NL80211_IFTYPE_STATION), },
	{ .max = 1,
	  .types = BIT(NL80211_IFTYPE_P2P_GO) | BIT(NL80211_IFTYPE_P2P_CLIENT) |
		   BIT(NL80211_IFTYPE_AP), },
};
static const struct ieee80211_iface_combination ssv6xxx_iface_combinations_p2p
    [] = { { .num_different_channels = 1,
	     .max_interfaces = SSV6200_MAX_VIF,
	     .beacon_int_infra_match = true,
	     .limits = ssv6xxx_p2p_limits,
	     .n_limits = ARRAY_SIZE(ssv6xxx_p2p_limits), }, };
#define LBYTESWAP(a) ((((a) & 0x00ff00ff) << 8) | (((a) & 0xff00ff00) >> 8))
#define LONGSWAP(a) ((LBYTESWAP(a) << 16) | (LBYTESWAP(a) >> 16))
#define CHAN2G(_freq, _idx)                                                    \
	{                                                                      \
		.band = IEEE80211_BAND_2GHZ, .center_freq = (_freq),           \
		.hw_value = (_idx), .max_power = 20,                           \
	}
#ifndef WLAN_CIPHER_SUITE_SMS4
#define WLAN_CIPHER_SUITE_SMS4 0x00147201
#endif
#define SHPCHECK(__hw_rate, __flags)                                           \
	((__flags &IEEE80211_RATE_SHORT_PREAMBLE) ? (__hw_rate + 3) : 0)
#define RATE(_bitrate, _hw_rate, _flags)                                       \
	{                                                                      \
		.bitrate = (_bitrate), .flags = (_flags),                      \
		.hw_value = (_hw_rate),                                        \
		.hw_value_short = SHPCHECK(_hw_rate, _flags)                   \
	}
extern struct ssv6xxx_cfg ssv_cfg;
static const struct ieee80211_channel ssv6200_2ghz_chantable[] = {
	CHAN2G(2412, 1),  CHAN2G(2417, 2),  CHAN2G(2422, 3),  CHAN2G(2427, 4),
	CHAN2G(2432, 5),  CHAN2G(2437, 6),  CHAN2G(2442, 7),  CHAN2G(2447, 8),
	CHAN2G(2452, 9),  CHAN2G(2457, 10), CHAN2G(2462, 11), CHAN2G(2467, 12),
	CHAN2G(2472, 13), CHAN2G(2484, 14),
};
static struct ieee80211_rate ssv6200_legacy_rates[] = {
	RATE(10, 0x00, 0),
	RATE(20, 0x01, IEEE80211_RATE_SHORT_PREAMBLE),
	RATE(55, 0x02, IEEE80211_RATE_SHORT_PREAMBLE),
	RATE(110, 0x03, IEEE80211_RATE_SHORT_PREAMBLE),
	RATE(60, 0x07, 0),
	RATE(90, 0x08, 0),
	RATE(120, 0x09, 0),
	RATE(180, 0x0a, 0),
	RATE(240, 0x0b, 0),
	RATE(360, 0x0c, 0),
	RATE(480, 0x0d, 0),
	RATE(540, 0x0e, 0),
};
#ifdef CONFIG_SSV_CABRIO_E
int ssv6xxx_do_iq_calib(struct ssv_hw *sh, struct ssv6xxx_iqk_cfg *p_cfg)
{
	struct sk_buff *skb;
	struct cfg_host_cmd *host_cmd;
	printk("# Do init_cali (iq)\n");
	skb = ssv_skb_alloc(HOST_CMD_HDR_LEN + IQK_CFG_LEN + PHY_SETTING_SIZE +
			    RF_SETTING_SIZE);
	if (skb == NULL) {
		printk("init ssv6xxx_do_iq_calib fail!!!\n");
		return 0;
	}
	if ((PHY_SETTING_SIZE > MAX_PHY_SETTING_TABLE_SIZE) ||
	    (RF_SETTING_SIZE > MAX_RF_SETTING_TABLE_SIZE)) {
		printk("Please recheck RF or PHY table size!!!\n");
		BUG_ON(1);
		return 0;
	}
	skb->data_len =
	    HOST_CMD_HDR_LEN + IQK_CFG_LEN + PHY_SETTING_SIZE + RF_SETTING_SIZE;
	skb->len = skb->data_len;
	host_cmd = (struct cfg_host_cmd *)skb->data;
	host_cmd->c_type = HOST_CMD;
	host_cmd->h_cmd = (u8)SSV6XXX_HOST_CMD_INIT_CALI;
	host_cmd->len = skb->data_len;
	p_cfg->phy_tbl_size = PHY_SETTING_SIZE;
	p_cfg->rf_tbl_size = RF_SETTING_SIZE;
	memcpy(host_cmd->dat32, p_cfg, IQK_CFG_LEN);
	memcpy(host_cmd->dat8 + IQK_CFG_LEN, phy_setting, PHY_SETTING_SIZE);
	memcpy(host_cmd->dat8 + IQK_CFG_LEN + PHY_SETTING_SIZE, ssv6200_rf_tbl,
	       RF_SETTING_SIZE);
	sh->hci.hci_ops->hci_send_cmd(skb);
	ssv_skb_free(skb);
	mdelay(50);
	return 0;
}
#endif
#define HT_CAP_RX_STBC_ONE_STREAM 0x1
#ifdef CONFIG_SSV_WAPI
static const u32 ssv6xxx_cipher_suites[] = {
	WLAN_CIPHER_SUITE_WEP40, WLAN_CIPHER_SUITE_WEP104,
	WLAN_CIPHER_SUITE_TKIP,  WLAN_CIPHER_SUITE_CCMP,
	WLAN_CIPHER_SUITE_SMS4,  WLAN_CIPHER_SUITE_AES_CMAC
};
#endif
static void ssv6xxx_set_80211_hw_capab(struct ssv_softc *sc)
{
	struct ieee80211_hw *hw = sc->hw;
	struct ssv_hw *sh = sc->sh;
	struct ieee80211_sta_ht_cap *ht_info;
#ifdef CONFIG_SSV_WAPI
	hw->wiphy->cipher_suites = ssv6xxx_cipher_suites;
	hw->wiphy->n_cipher_suites = ARRAY_SIZE(ssv6xxx_cipher_suites);
#endif
	hw->flags = IEEE80211_HW_SIGNAL_DBM;
#ifdef CONFIG_SSV_SUPPORT_ANDROID
#endif
	hw->rate_control_algorithm = "ssv6xxx_rate_control";
	ht_info = &sc->sbands[IEEE80211_BAND_2GHZ].ht_cap;
	ampdu_db_log("sh->cfg.hw_caps = 0x%x\n", sh->cfg.hw_caps);
	if (sh->cfg.hw_caps & SSV6200_HW_CAP_HT) {
		if (sh->cfg.hw_caps & SSV6200_HW_CAP_AMPDU_RX) {
			hw->flags |= IEEE80211_HW_AMPDU_AGGREGATION;
			ampdu_db_log(
			    "set IEEE80211_HW_AMPDU_AGGREGATION(0x%x)\n",
			    ((hw->flags) & IEEE80211_HW_AMPDU_AGGREGATION));
		}
		ht_info->cap = IEEE80211_HT_CAP_SM_PS;
		if (sh->cfg.hw_caps & SSV6200_HW_CAP_GF)
			ht_info->cap |= IEEE80211_HT_CAP_GRN_FLD;
		if (sh->cfg.hw_caps & SSV6200_HT_CAP_SGI_20)
			ht_info->cap |= IEEE80211_HT_CAP_SGI_20;
		ht_info->ampdu_factor = IEEE80211_HT_MAX_AMPDU_32K;
		ht_info->ampdu_density = IEEE80211_HT_MPDU_DENSITY_8;
		memset(&ht_info->mcs, 0, sizeof(ht_info->mcs));
		ht_info->mcs.rx_mask[0] = 0xff;
		ht_info->mcs.tx_params |= IEEE80211_HT_MCS_TX_DEFINED;
		ht_info->mcs.rx_highest = cpu_to_le16(SSV6200_RX_HIGHEST_RATE);
		ht_info->ht_supported = true;
	}
	hw->wiphy->interface_modes = BIT(NL80211_IFTYPE_STATION);
#if LINUX_VERSION_CODE >= KERNEL_VERSION(3, 0, 0)
	if (sh->cfg.hw_caps & SSV6200_HW_CAP_P2P) {
		hw->wiphy->interface_modes |= BIT(NL80211_IFTYPE_P2P_CLIENT);
		hw->wiphy->interface_modes |= BIT(NL80211_IFTYPE_P2P_GO);
		hw->wiphy->iface_combinations = ssv6xxx_iface_combinations_p2p;
		hw->wiphy->n_iface_combinations =
		    ARRAY_SIZE(ssv6xxx_iface_combinations_p2p);
#if LINUX_VERSION_CODE < KERNEL_VERSION(3, 5, 0)
		hw->wiphy->flags |= WIPHY_FLAG_ENFORCE_COMBINATIONS;
#endif
	}
#endif
#if LINUX_VERSION_CODE > KERNEL_VERSION(3, 5, 0)
	hw->wiphy->flags |= WIPHY_FLAG_HAS_REMAIN_ON_CHANNEL;
#endif
	if (sh->cfg.hw_caps & SSV6200_HW_CAP_AP) {
		hw->wiphy->interface_modes |= BIT(NL80211_IFTYPE_AP);
		hw->wiphy->flags |= WIPHY_FLAG_AP_UAPSD;
	}
	hw->queues = 4;
	hw->max_rates = 4;
#if LINUX_VERSION_CODE < KERNEL_VERSION(3, 14, 0)
	hw->channel_change_time = 5000;
#endif
	hw->max_listen_interval = 1;
	hw->max_rate_tries = HW_MAX_RATE_TRIES;
	hw->extra_tx_headroom = TXPB_OFFSET + AMPDU_DELIMITER_LEN;
	if (sizeof(struct ampdu_hdr_st) > SSV_SKB_info_size)
		hw->extra_tx_headroom += sizeof(struct ampdu_hdr_st);
	else
		hw->extra_tx_headroom += SSV_SKB_info_size;
	if (sh->cfg.hw_caps & SSV6200_HW_CAP_2GHZ) {
		hw->wiphy->bands[IEEE80211_BAND_2GHZ] =
		    &sc->sbands[IEEE80211_BAND_2GHZ];
	}
#if LINUX_VERSION_CODE >= KERNEL_VERSION(3, 0, 0)
	if (sh->cfg.hw_caps & SSV6200_HW_CAP_AMPDU_TX)
#ifdef PREFER_RX
		hw->max_rx_aggregation_subframes = 64;
#else
		hw->max_rx_aggregation_subframes = 16;
#endif
	else
		hw->max_rx_aggregation_subframes = 12;
	hw->max_tx_aggregation_subframes = 64;
#endif
	hw->sta_data_size = sizeof(struct ssv_sta_priv_data);
	hw->vif_data_size = sizeof(struct ssv_vif_priv_data);
	memcpy(sh->maddr[0].addr, &sh->cfg.maddr[0][0], ETH_ALEN);
	hw->wiphy->addresses = sh->maddr;
	hw->wiphy->n_addresses = 1;
	if (sh->cfg.hw_caps & SSV6200_HW_CAP_P2P) {
		int i;
		for (i = 1; i < SSV6200_MAX_HW_MAC_ADDR; i++) {
			memcpy(sh->maddr[i].addr, sh->maddr[i - 1].addr,
			       ETH_ALEN);
			sh->maddr[i].addr[5]++;
			hw->wiphy->n_addresses++;
		}
	}
	if (!is_zero_ether_addr(sh->cfg.maddr[1])) {
		memcpy(sh->maddr[1].addr, sh->cfg.maddr[1], ETH_ALEN);
		if (hw->wiphy->n_addresses < 2)
			hw->wiphy->n_addresses = 2;
	}
	hw->wiphy->wowlan.flags = WIPHY_WOWLAN_DISCONNECT;
}
#ifdef MULTI_THREAD_ENCRYPT
extern struct list_head encrypt_task_head;
int ssv6xxx_cpu_callback(struct notifier_block *nfb, unsigned long action,
			 void *hcpu)
{
	int hotcpu = (unsigned long)hcpu;
	struct ssv_encrypt_task_list *ta = NULL;
	switch (action) {
	case CPU_UP_PREPARE:
	case CPU_UP_PREPARE_FROZEN:
	case CPU_DOWN_FAILED: {
		int cpu = 0;
		list_for_each_entry(ta, &encrypt_task_head, list)
		{
			if (cpu == hotcpu)
				break;
			cpu++;
		}
		if (ta->encrypt_task->state & TASK_UNINTERRUPTIBLE) {
			kthread_bind(ta->encrypt_task, hotcpu);
		}
		printk("encrypt_task %p state is %ld\n", ta->encrypt_task,
		       ta->encrypt_task->state);
		break;
	}
	case CPU_ONLINE:
	case CPU_ONLINE_FROZEN: {
		int cpu = 0;
		list_for_each_entry(ta, &encrypt_task_head, list)
		{
			if (cpu == hotcpu) {
				ta->cpu_offline = 0;
				if ((ta->started == 0) && (cpu_online(cpu))) {
					wake_up_process(ta->encrypt_task);
					ta->started = 1;
					printk("wake up encrypt_task %p state "
					       "is %ld, cpu = %d\n",
					       ta->encrypt_task,
					       ta->encrypt_task->state, cpu);
				}
				break;
			}
			cpu++;
		}
		printk("encrypt_task %p state is %ld\n", ta->encrypt_task,
		       ta->encrypt_task->state);
		break;
	}
#ifdef CONFIG_HOTPLUG_CPU
	case CPU_UP_CANCELED:
	case CPU_UP_CANCELED_FROZEN:
	case CPU_DOWN_PREPARE: {
		int cpu = 0;
		list_for_each_entry(ta, &encrypt_task_head, list)
		{
			if (cpu == hotcpu) {
				ta->cpu_offline = 1;
				break;
			}
			cpu++;
		}
		printk("p = %p\n", ta->encrypt_task);
		break;
	}
	case CPU_DEAD:
	case CPU_DEAD_FROZEN: {
		break;
	}
#endif
	}
	return NOTIFY_OK;
}
static struct notifier_block cpu_nfb = { .notifier_call =
					     ssv6xxx_cpu_callback };
#endif

void ssv6xxx_watchdog_restart_hw(struct ssv_softc *sc)
{
    printk("%s(): \n", __FUNCTION__);

	//printk("**************************\n");
    //printk("*** Software MAC reset ***\n");
    //printk("**************************\n");

    sc->restart_counter++;
    sc->force_triger_reset = true;
    //HCI_STOP(sc->sh);
    //Disable PHY
    //SMAC_REG_WRITE(sc->sh, 0xce000004, 0x0);
    sc->beacon_info[0].pubf_addr = 0x00;
    sc->beacon_info[1].pubf_addr = 0x00;
    //SMAC_REG_WRITE(sc->sh, ADR_BRG_SW_RST, 0x0);
    ieee80211_restart_hw(sc->hw);
}

void ssv6200_watchdog_timeout(unsigned long arg)
{
    struct ssv_softc *sc = (struct ssv_softc *)arg;
    //printk("@@ ssv6200_watchdog_timeout @@\n");
    if(sc->watchdog_flag == 0) 
        ssv6xxx_watchdog_restart_hw(sc);

    sc->watchdog_flag = 0;
    mod_timer(&sc->watchdog_timeout,jiffies + 5*HZ);

    return;
}

static int ssv6xxx_init_softc(struct ssv_softc *sc)
{
	void *channels;
	int ret = 0;
#ifdef MULTI_THREAD_ENCRYPT
	unsigned int cpu;
#endif
	sc->sc_flags = SC_OP_INVALID;
	mutex_init(&sc->mutex);
	mutex_init(&sc->mem_mutex);
	sc->config_wq = create_singlethread_workqueue("ssv6xxx_cong_wq");
	INIT_WORK(&sc->set_tim_work, ssv6200_set_tim_work);
	INIT_WORK(&sc->bcast_start_work, ssv6200_bcast_start_work);
	INIT_DELAYED_WORK(&sc->bcast_stop_work, ssv6200_bcast_stop_work);
	INIT_DELAYED_WORK(&sc->bcast_tx_work, ssv6200_bcast_tx_work);
	INIT_WORK(&sc->set_ampdu_rx_add_work, ssv6xxx_set_ampdu_rx_add_work);
	INIT_WORK(&sc->set_ampdu_rx_del_work, ssv6xxx_set_ampdu_rx_del_work);
#ifdef CONFIG_SSV_SUPPORT_ANDROID
#ifdef CONFIG_HAS_EARLYSUSPEND
	sc->early_suspend.level = EARLY_SUSPEND_LEVEL_BLANK_SCREEN + 20;
	sc->early_suspend.suspend = ssv6xxx_early_suspend;
	sc->early_suspend.resume = ssv6xxx_late_resume;
	register_early_suspend(&sc->early_suspend);
#endif
	ssv_wakelock_init(sc);
#endif
	sc->mac_deci_tbl = sta_deci_tbl;
	memset((void *)&sc->tx, 0, sizeof(struct ssv_tx));
	sc->tx.hw_txqid[WMM_AC_VO] = 3;
	sc->tx.ac_txqid[3] = WMM_AC_VO;
	sc->tx.hw_txqid[WMM_AC_VI] = 2;
	sc->tx.ac_txqid[2] = WMM_AC_VI;
	sc->tx.hw_txqid[WMM_AC_BE] = 1;
	sc->tx.ac_txqid[1] = WMM_AC_BE;
	sc->tx.hw_txqid[WMM_AC_BK] = 0;
	sc->tx.ac_txqid[0] = WMM_AC_BK;
	INIT_LIST_HEAD(&sc->tx.ampdu_tx_que);
	spin_lock_init(&sc->tx.ampdu_tx_que_lock);
	memset((void *)&sc->rx, 0, sizeof(struct ssv_rx));
	spin_lock_init(&sc->rx.rxq_lock);
	skb_queue_head_init(&sc->rx.rxq_head);
	sc->rx.rx_buf = ssv_skb_alloc(MAX_FRAME_SIZE);
	if (sc->rx.rx_buf == NULL)
		return -ENOMEM;
	memset(&sc->bcast_txq, 0, sizeof(struct ssv6xxx_bcast_txq));
	spin_lock_init(&sc->bcast_txq.txq_lock);
	skb_queue_head_init(&sc->bcast_txq.qhead);
	spin_lock_init(&sc->ps_state_lock);
#ifdef CONFIG_P2P_NOA
	spin_lock_init(&sc->p2p_noa.p2p_config_lock);
#endif
	if (sc->sh->cfg.hw_caps & SSV6200_HW_CAP_2GHZ) {
		channels = kmemdup(ssv6200_2ghz_chantable,
				   sizeof(ssv6200_2ghz_chantable), GFP_KERNEL);
		if (!channels) {
			kfree(sc->rx.rx_buf);
			return -ENOMEM;
		}
		sc->sbands[IEEE80211_BAND_2GHZ].channels = channels;
		sc->sbands[IEEE80211_BAND_2GHZ].band = IEEE80211_BAND_2GHZ;
		sc->sbands[IEEE80211_BAND_2GHZ].n_channels =
		    ARRAY_SIZE(ssv6200_2ghz_chantable);
		sc->sbands[IEEE80211_BAND_2GHZ].bitrates = ssv6200_legacy_rates;
		sc->sbands[IEEE80211_BAND_2GHZ].n_bitrates =
		    ARRAY_SIZE(ssv6200_legacy_rates);
	}
	sc->cur_channel = NULL;
	sc->hw_chan = (-1);
	ssv6xxx_set_80211_hw_capab(sc);
	ret = ssv6xxx_rate_control_register();
	if (ret != 0) {
		printk("%s(): Failed to register rc algorithm.\n",
		       __FUNCTION__);
	}
#ifdef MULTI_THREAD_ENCRYPT
	skb_queue_head_init(&sc->preprocess_q);
	skb_queue_head_init(&sc->crypted_q);
	spin_lock_init(&sc->crypt_st_lock);
	INIT_LIST_HEAD(&encrypt_task_head);
	for_each_cpu(cpu, cpu_present_mask)
	{
		struct ssv_encrypt_task_list *ta =
		    kzalloc(sizeof(*ta), GFP_KERNEL);
		memset(ta, 0, sizeof(*ta));
		ta->encrypt_task = kthread_create_on_node(
		    ssv6xxx_encrypt_task, sc, cpu_to_node(cpu),
		    "%d/ssv6xxx_encrypt_task", cpu);
		init_waitqueue_head(&ta->encrypt_wait_q);
		if (!IS_ERR(ta->encrypt_task)) {
			printk("[MT-ENCRYPT]: create kthread %p for CPU %d, "
			       "ret = %d\n",
			       ta->encrypt_task, cpu, ret);
			kthread_bind(ta->encrypt_task, cpu);
			list_add_tail(&ta->list, &encrypt_task_head);
			if (cpu_online(cpu)) {
				wake_up_process(ta->encrypt_task);
				ta->started = 1;
			}
			ta->cpu_no = cpu;
		} else {
			printk("[MT-ENCRYPT]: Fail to create kthread\n");
		}
	}
	register_cpu_notifier(&cpu_nfb);
#endif
	init_waitqueue_head(&sc->tx_wait_q);
	sc->tx_wait_q_woken = 0;
	skb_queue_head_init(&sc->tx_skb_q);
	sc->tx_task = kthread_run(ssv6xxx_tx_task, sc, "ssv6xxx_tx_task");
	sc->tx_q_empty = false;
	skb_queue_head_init(&sc->tx_done_q);
	init_waitqueue_head(&sc->rx_wait_q);
	sc->rx_wait_q_woken = 0;
	skb_queue_head_init(&sc->rx_skb_q);
	sc->rx_task = kthread_run(ssv6xxx_rx_task, sc, "ssv6xxx_rx_task");

        //use to host reset
    init_timer(&sc->watchdog_timeout); 
    sc->watchdog_timeout.expires = jiffies + 5*HZ;
    sc->watchdog_timeout.data = (unsigned long)sc;
    sc->watchdog_timeout.function = ssv6200_watchdog_timeout;

    add_timer(&sc->watchdog_timeout);
    
	return ret;
}
static int ssv6xxx_deinit_softc(struct ssv_softc *sc)
{
	void *channels;
	struct sk_buff *skb;
	u8 remain_size;
#ifdef MULTI_THREAD_ENCRYPT
	struct ssv_encrypt_task_list *qtask = NULL;
	int counter = 0;
#endif
	printk("%s():\n", __FUNCTION__);
	if (sc->sh->cfg.hw_caps & SSV6200_HW_CAP_2GHZ) {
		channels = sc->sbands[IEEE80211_BAND_2GHZ].channels;
		kfree(channels);
	}
#ifdef CONFIG_SSV_SUPPORT_ANDROID
#ifdef CONFIG_HAS_EARLYSUSPEND
	unregister_early_suspend(&sc->early_suspend);
#endif
	ssv_wakelock_destroy(sc);
#endif
	ssv_skb_free(sc->rx.rx_buf);
	sc->rx.rx_buf = NULL;
	ssv6xxx_rate_control_unregister();
	cancel_delayed_work_sync(&sc->bcast_tx_work);
    
    del_timer_sync(&sc->watchdog_timeout);
	do {
		skb = ssv6200_bcast_dequeue(&sc->bcast_txq, &remain_size);
		if (skb)
			ssv6xxx_txbuf_free_skb(skb, (void *)sc);
		else
			break;
	} while (remain_size);
#ifdef MULTI_THREAD_ENCRYPT
	unregister_cpu_notifier(&cpu_nfb);
	if (!list_empty(&encrypt_task_head)) {
		for (qtask = list_entry((&encrypt_task_head)->next,
					typeof(*qtask), list);
		     !list_empty(&encrypt_task_head);
		     qtask = list_entry((&encrypt_task_head)->next,
					typeof(*qtask), list)) {
			counter++;
			printk("Stopping encrypt task %d: ...\n", counter);
			kthread_stop(qtask->encrypt_task);
			printk("Stopped encrypt task %d: ...\n", counter);
			list_del(&qtask->list);
			kfree(qtask);
		}
	}
#endif
    if (sc->tx_task != NULL)
    {
        printk("Stopping TX task...\n");
        kthread_stop(sc->tx_task);
		sc->tx_task = NULL;
        printk("Stopped TX task.\n");        
    }
    if (sc->rx_task != NULL)
    {
        printk("Stopping RX task...\n");
        kthread_stop(sc->rx_task);
		sc->rx_task = NULL;
        printk("Stopped RX task.\n");    
    }
#ifdef MULTI_THREAD_ENCRYPT
	while ((skb = skb_dequeue(&sc->preprocess_q)) != NULL) {
		SKB_info *skb_info = (SKB_info *)skb->head;
		if (skb_info->crypt_st == PKT_CRYPT_ST_ENC_PRE)
			ssv6xxx_txbuf_free_skb(skb, (void *)sc);
		else
			dev_kfree_skb_any(skb);
	}
	while ((skb = skb_dequeue(&sc->crypted_q)) != NULL) {
		SKB_info *skb_info = (SKB_info *)skb->head;
		if (skb_info->crypt_st == PKT_CRYPT_ST_ENC_DONE)
			ssv6xxx_txbuf_free_skb(skb, (void *)sc);
		else
			dev_kfree_skb_any(skb);
	}
	printk("[MT-ENCRYPT]: end of de-init\n");
#endif
	destroy_workqueue(sc->config_wq);
	return 0;
}
static void ssv6xxx_hw_set_replay_ignore(struct ssv_hw *sh, u8 ignore)
{
	u32 temp;
	SMAC_REG_READ(sh, ADR_SCRT_SET, &temp);
	temp = temp & SCRT_RPLY_IGNORE_I_MSK;
	temp |= (ignore << SCRT_RPLY_IGNORE_SFT);
	SMAC_REG_WRITE(sh, ADR_SCRT_SET, temp);
}
int ssv6xxx_init_mac(struct ssv_hw *sh)
{
	struct ssv_softc *sc = sh->sc;
	int i, ret = 0;
#ifdef SSV6200_ECO
    u32 *ptr, id_len, regval, temp[0x8];
#else
    u32 *ptr, id_len, regval;
#endif
	char *chip_id = sh->chip_id;
	printk(KERN_INFO "SVN version %d\n", ssv_root_version);
	printk(KERN_INFO "SVN ROOT URL %s \n", SSV_ROOT_URl);
	printk(KERN_INFO "COMPILER HOST %s \n", COMPILERHOST);
	printk(KERN_INFO "COMPILER DATE %s \n", COMPILERDATE);
	printk(KERN_INFO "COMPILER OS %s \n", COMPILEROS);
	printk(KERN_INFO "COMPILER OS ARCH %s \n", COMPILEROSARCH);
	SMAC_REG_READ(sh, ADR_IC_TIME_TAG_1, &regval);
	sh->chip_tag = ((u64)regval << 32);
	SMAC_REG_READ(sh, ADR_IC_TIME_TAG_0, &regval);
	sh->chip_tag |= (regval);
	printk(KERN_INFO "CHIP TAG: %llx \n", sh->chip_tag);
	SMAC_REG_READ(sh, ADR_CHIP_ID_3, &regval);
	*((u32 *)&chip_id[0]) = (u32)LONGSWAP(regval);
	SMAC_REG_READ(sh, ADR_CHIP_ID_2, &regval);
	*((u32 *)&chip_id[4]) = (u32)LONGSWAP(regval);
	SMAC_REG_READ(sh, ADR_CHIP_ID_1, &regval);
	*((u32 *)&chip_id[8]) = (u32)LONGSWAP(regval);
	SMAC_REG_READ(sh, ADR_CHIP_ID_0, &regval);
	*((u32 *)&chip_id[12]) = (u32)LONGSWAP(regval);
	chip_id[12 + sizeof(u32)] = 0;
	printk(KERN_INFO "CHIP ID: %s \n", chip_id);
	if (sc->ps_status == PWRSV_ENABLE) {

#ifdef CONFIG_SSV_HW_ENCRYPT_SW_DECRYPT
		SMAC_REG_WRITE(sh, ADR_RX_FLOW_DATA,
			       M_ENG_MACRX | (M_ENG_HWHCI << 4));
#else
		SMAC_REG_WRITE(sh, ADR_RX_FLOW_DATA,
			       M_ENG_MACRX | (M_ENG_ENCRYPT_SEC << 4) |
				   (M_ENG_HWHCI << 8));
#endif
		SMAC_REG_WRITE(sc->sh, ADR_RX_FLOW_MNG,
			       M_ENG_MACRX | (M_ENG_HWHCI << 4));
#if Enable_AMPDU_FW_Retry
		SMAC_REG_WRITE(sh, ADR_RX_FLOW_CTRL, M_ENG_MACRX |
							 (M_ENG_CPU << 4) |
							 (M_ENG_HWHCI << 8));
#else
		SMAC_REG_WRITE(sh, ADR_RX_FLOW_CTRL,
			       M_ENG_MACRX | (M_ENG_HWHCI << 4));
#endif
		SMAC_REG_WRITE(sc->sh, ADR_MRX_FLT_TB0 + 6 * 4,
			       (sc->mac_deci_tbl[6]));
		return ret;
	}
    SMAC_REG_SET_BITS(sh, ADR_PHY_EN_1, (0 << RG_PHY_MD_EN_SFT), RG_PHY_MD_EN_MSK);
	SMAC_REG_WRITE(sh, ADR_BRG_SW_RST, 1 << 1);
	SMAC_REG_WRITE(sc->sh, ADR_TXQ4_MTX_Q_AIFSN, 0xffff2101);
	SMAC_REG_SET_BITS(sc->sh, ADR_MTX_BCN_EN_MISC, 0,
			  MTX_HALT_MNG_UNTIL_DTIM_MSK);
	SMAC_REG_WRITE(sh, ADR_CONTROL, 0x12000006);
	SMAC_REG_WRITE(sh, ADR_RX_TIME_STAMP_CFG,
		       ((28 << MRX_STP_OFST_SFT) | 0x01));
	SMAC_REG_WRITE(sh, ADR_HCI_TX_RX_INFO_SIZE,
		       ((u32)(TXPB_OFFSET) << TX_PBOFFSET_SFT) |
			   ((u32)(sh->tx_desc_len) << TX_INFO_SIZE_SFT) |
			   ((u32)(sh->rx_desc_len) << RX_INFO_SIZE_SFT) |
			   ((u32)(sh->rx_pinfo_pad) << RX_LAST_PHY_SIZE_SFT));
	SMAC_REG_READ(sh, ADR_MMU_CTRL, &regval);
	regval |= (0xff << MMU_SHARE_MCU_SFT);
	SMAC_REG_WRITE(sh, ADR_MMU_CTRL, regval);
	SMAC_REG_READ(sh, ADR_MRX_WATCH_DOG, &regval);
	regval &= 0xfffffff0;
	SMAC_REG_WRITE(sh, ADR_MRX_WATCH_DOG, regval);
	SMAC_REG_READ(sh, ADR_TRX_ID_THRESHOLD, &id_len);
	id_len = (id_len & 0xffff0000) |
		 (SSV6200_ID_TX_THRESHOLD << TX_ID_THOLD_SFT) |
		 (SSV6200_ID_RX_THRESHOLD << RX_ID_THOLD_SFT);
	SMAC_REG_WRITE(sh, ADR_TRX_ID_THRESHOLD, id_len);
	SMAC_REG_READ(sh, ADR_ID_LEN_THREADSHOLD1, &id_len);
	id_len = (id_len & 0x0f) |
		 (SSV6200_PAGE_TX_THRESHOLD << ID_TX_LEN_THOLD_SFT) |
		 (SSV6200_PAGE_RX_THRESHOLD << ID_RX_LEN_THOLD_SFT);
	SMAC_REG_WRITE(sh, ADR_ID_LEN_THREADSHOLD1, id_len);
#ifdef CONFIG_SSV_CABRIO_MB_DEBUG
	SMAC_REG_READ(sh, ADR_MB_DBG_CFG3, &regval);
	regval |= (debug_buffer << 0);
	SMAC_REG_WRITE(sh, ADR_MB_DBG_CFG3, regval);
	SMAC_REG_READ(sh, ADR_MB_DBG_CFG2, &regval);
	regval |= (DEBUG_SIZE << 16);
	SMAC_REG_WRITE(sh, ADR_MB_DBG_CFG2, regval);
	SMAC_REG_READ(sh, ADR_MB_DBG_CFG1, &regval);
	regval |= (1 << MB_DBG_EN_SFT);
	SMAC_REG_WRITE(sh, ADR_MB_DBG_CFG1, regval);
	SMAC_REG_READ(sh, ADR_MBOX_HALT_CFG, &regval);
	regval |= (1 << MB_ERR_AUTO_HALT_EN_SFT);
	SMAC_REG_WRITE(sh, ADR_MBOX_HALT_CFG, regval);
#endif
	SMAC_REG_READ(sc->sh, ADR_MTX_BCN_EN_MISC, &regval);
	regval |= (1 << MTX_TSF_TIMER_EN_SFT);
	SMAC_REG_WRITE(sc->sh, ADR_MTX_BCN_EN_MISC, regval);
#ifdef SSV6200_ECO
	SMAC_REG_WRITE(sh, 0xcd010004, 0x1213);
	for (i = 0; i < SSV_RC_MAX_STA; i++) {
		if (i == 0) {
			sh->hw_buf_ptr[i] = ssv6xxx_pbuf_alloc(
			    sc, sizeof(phy_info_tbl) +
				    sizeof(struct ssv6xxx_hw_sec),
			    NOTYPE_BUF);
			if ((sh->hw_buf_ptr[i] >> 28) != 8) {
				printk("opps allocate pbuf error\n");
				WARN_ON(1);
				ret = 1;
				goto exit;
			}
		} else {
			sh->hw_buf_ptr[i] = ssv6xxx_pbuf_alloc(
			    sc, sizeof(struct ssv6xxx_hw_sec), NOTYPE_BUF);
			if ((sh->hw_buf_ptr[i] >> 28) != 8) {
				printk("opps allocate pbuf error\n");
				WARN_ON(1);
				ret = 1;
				goto exit;
			}
		}
		printk("%s(): ssv6200 reserved space=0x%08x\n", __FUNCTION__,
		       sh->hw_buf_ptr[i]);
	}
    for (i = 0; i < 0x8; i++) {
        temp[i] = 0;
        temp[i] = ssv6xxx_pbuf_alloc(sc, 256,NOTYPE_BUF);
    }

    for (i = 0; i < 0x8; i++) {
        if(temp[i] == 0x800e0000)
            printk("0x800e0000\n");
        else
            ssv6xxx_pbuf_free(sc,temp[i]);
    }
#else
	sh->hw_buf_ptr = ssv6xxx_pbuf_alloc(
	    sc, sizeof(phy_info_tbl) + sizeof(struct ssv6xxx_hw_sec),
	    NOTYPE_BUF);
	if ((sh->hw_buf_ptr >> 28) != 8) {
		printk("opps allocate pbuf error\n");
		WARN_ON(1);
		ret = 1;
		goto exit;
	}
	printk("%s(): ssv6200 reserved space=0x%08x, size=%d\n", __FUNCTION__,
	       sh->hw_buf_ptr,
	       (u32)(sizeof(phy_info_tbl) + sizeof(struct ssv6xxx_hw_sec)));
#endif
#ifdef SSV6200_ECO
	for (i = 0; i < SSV_RC_MAX_STA; i++)
		sh->hw_sec_key[i] = sh->hw_buf_ptr[i];
	for (i = 0; i < SSV_RC_MAX_STA; i++) {
		int x;
		for (x = 0; x < sizeof(struct ssv6xxx_hw_sec); x += 4) {
			SMAC_REG_WRITE(sh, sh->hw_sec_key[i] + x, 0);
		}
	}
	SMAC_REG_READ(sh, ADR_SCRT_SET, &regval);
	regval &= SCRT_PKT_ID_I_MSK;
	regval |= ((sh->hw_sec_key[0] >> 16) << SCRT_PKT_ID_SFT);
	SMAC_REG_WRITE(sh, ADR_SCRT_SET, regval);
	sh->hw_pinfo = sh->hw_sec_key[0] + sizeof(struct ssv6xxx_hw_sec);
	for (i = 0, ptr = phy_info_tbl; i < PHY_INFO_TBL1_SIZE; i++, ptr++) {
		SMAC_REG_WRITE(sh, ADR_INFO0 + i * 4, *ptr);
		SMAC_REG_CONFIRM(sh, ADR_INFO0 + i * 4, *ptr);
	}
#else
	sh->hw_sec_key = sh->hw_buf_ptr;
	for (i = 0; i < sizeof(struct ssv6xxx_hw_sec); i += 4) {
		SMAC_REG_WRITE(sh, sh->hw_sec_key + i, 0);
	}
	SMAC_REG_READ(sh, ADR_SCRT_SET, &regval);
	regval &= SCRT_PKT_ID_I_MSK;
	regval |= ((sh->hw_sec_key >> 16) << SCRT_PKT_ID_SFT);
	SMAC_REG_WRITE(sh, ADR_SCRT_SET, regval);
	sh->hw_pinfo = sh->hw_sec_key + sizeof(struct ssv6xxx_hw_sec);
	for (i = 0, ptr = phy_info_tbl; i < PHY_INFO_TBL1_SIZE; i++, ptr++) {
		SMAC_REG_WRITE(sh, ADR_INFO0 + i * 4, *ptr);
		SMAC_REG_CONFIRM(sh, ADR_INFO0 + i * 4, *ptr);
	}
#endif
	for (i = 0; i < PHY_INFO_TBL2_SIZE; i++, ptr++) {
		SMAC_REG_WRITE(sh, sh->hw_pinfo + i * 4, *ptr);
		SMAC_REG_CONFIRM(sh, sh->hw_pinfo + i * 4, *ptr);
	}
	for (i = 0; i < PHY_INFO_TBL3_SIZE; i++, ptr++) {
		SMAC_REG_WRITE(
		    sh, sh->hw_pinfo + (PHY_INFO_TBL2_SIZE << 2) + i * 4, *ptr);
		SMAC_REG_CONFIRM(
		    sh, sh->hw_pinfo + (PHY_INFO_TBL2_SIZE << 2) + i * 4, *ptr);
	}
	SMAC_REG_WRITE(sh, ADR_INFO_RATE_OFFSET, 0x00040000);
	SMAC_REG_WRITE(sh, ADR_INFO_IDX_ADDR, sh->hw_pinfo);
	SMAC_REG_WRITE(sh, ADR_INFO_LEN_ADDR,
		       sh->hw_pinfo + (PHY_INFO_TBL2_SIZE) * 4);
	printk("ADR_INFO_IDX_ADDR[%08x] ADR_INFO_LEN_ADDR[%08x]\n",
	       sh->hw_pinfo, sh->hw_pinfo + (PHY_INFO_TBL2_SIZE) * 4);
	SMAC_REG_WRITE(sh, ADR_GLBLE_SET,
		       (0 << OP_MODE_SFT) | (0 << SNIFFER_MODE_SFT) |
			   (1 << DUP_FLT_SFT) |
			   (SSV6200_TX_PKT_RSVD_SETTING << TX_PKT_RSVD_SFT) |
			   ((u32)(RXPB_OFFSET) << PB_OFFSET_SFT));
	SMAC_REG_WRITE(sh, ADR_STA_MAC_0, *((u32 *)&sh->cfg.maddr[0][0]));
	SMAC_REG_WRITE(sh, ADR_STA_MAC_1, *((u32 *)&sh->cfg.maddr[0][4]));
	SMAC_REG_WRITE(sh, ADR_BSSID_0, *((u32 *)&sc->bssid[0]));
	SMAC_REG_WRITE(sh, ADR_BSSID_1, *((u32 *)&sc->bssid[4]));
	SMAC_REG_WRITE(sh, ADR_TX_ETHER_TYPE_0, 0x00000000);
	SMAC_REG_WRITE(sh, ADR_TX_ETHER_TYPE_1, 0x00000000);
	SMAC_REG_WRITE(sh, ADR_RX_ETHER_TYPE_0, 0x00000000);
	SMAC_REG_WRITE(sh, ADR_RX_ETHER_TYPE_1, 0x00000000);
    SMAC_REG_WRITE(sh, ADR_REASON_TRAP0, 0x7FBC7F87);
	SMAC_REG_WRITE(sh, ADR_REASON_TRAP1, 0x0000003F);
#ifndef FW_WSID_WATCH_LIST
	SMAC_REG_WRITE(sh, ADR_TRAP_HW_ID, M_ENG_HWHCI);
#else
	SMAC_REG_WRITE(sh, ADR_TRAP_HW_ID, M_ENG_CPU);
#endif
	SMAC_REG_WRITE(sh, ADR_WSID0, 0x00000000);
	SMAC_REG_WRITE(sh, ADR_WSID1, 0x00000000);
#ifdef CONFIG_SSV_HW_ENCRYPT_SW_DECRYPT
	SMAC_REG_WRITE(sh, ADR_RX_FLOW_DATA, M_ENG_MACRX | (M_ENG_HWHCI << 4));
#else
	SMAC_REG_WRITE(sh, ADR_RX_FLOW_DATA, M_ENG_MACRX |
						 (M_ENG_ENCRYPT_SEC << 4) |
						 (M_ENG_HWHCI << 8));
#endif
#ifdef CONFIG_P2P_NOA
	SMAC_REG_WRITE(sh, ADR_RX_FLOW_MNG,
		       M_ENG_MACRX | (M_ENG_CPU << 4) | (M_ENG_HWHCI << 8));
#else
	SMAC_REG_WRITE(sh, ADR_RX_FLOW_MNG, M_ENG_MACRX | (M_ENG_HWHCI << 4));
#endif
#if Enable_AMPDU_FW_Retry
	SMAC_REG_WRITE(sh, ADR_RX_FLOW_CTRL,
		       M_ENG_MACRX | (M_ENG_CPU << 4) | (M_ENG_HWHCI << 8));
#else
	SMAC_REG_WRITE(sh, ADR_RX_FLOW_CTRL, M_ENG_MACRX | (M_ENG_HWHCI << 4));
#endif
	ssv6xxx_hw_set_replay_ignore(sh, 1);
	ssv6xxx_update_decision_table(sc);
	SMAC_REG_SET_BITS(sc->sh, ADR_GLBLE_SET, SSV6200_OPMODE_STA,
			  OP_MODE_MSK);
	SMAC_REG_WRITE(sh, ADR_SDIO_MASK, 0xfffe1fff);
#ifdef CONFIG_SSV_TX_LOWTHRESHOLD
	SMAC_REG_WRITE(sh, ADR_TX_LIMIT_INTR,
		       0x80000000 | SSV6200_TX_LOWTHRESHOLD_ID_TRIGGER << 16 |
			   SSV6200_TX_LOWTHRESHOLD_PAGE_TRIGGER);
#else
	SMAC_REG_WRITE(sh, ADR_MB_THRESHOLD6, 0x80000000);
	SMAC_REG_WRITE(sh, ADR_MB_THRESHOLD8, 0x04020000);
	SMAC_REG_WRITE(sh, ADR_MB_THRESHOLD9, 0x00000404);
#endif
#ifdef CONFIG_SSV_SUPPORT_BTCX
	SMAC_REG_WRITE(sh, ADR_BTCX0, COEXIST_EN_MSK |
					  (WIRE_MODE_SZ << WIRE_MODE_SFT) |
					  WIFI_TX_SW_POL_MSK | BT_SW_POL_MSK);
	SMAC_REG_WRITE(sh, ADR_BTCX1,
		       SSV6200_BT_PRI_SMP_TIME |
			   (SSV6200_BT_STA_SMP_TIME << BT_STA_SMP_TIME_SFT) |
			   (SSV6200_WLAN_REMAIN_TIME << WLAN_REMAIN_TIME_SFT));
	SMAC_REG_WRITE(sh, ADR_SWITCH_CTL, BT_2WIRE_EN_MSK);
	SMAC_REG_WRITE(sh, ADR_PAD7, 1);
	SMAC_REG_WRITE(sh, ADR_PAD8, 0);
	SMAC_REG_WRITE(sh, ADR_PAD9, 1);
	SMAC_REG_WRITE(sh, ADR_PAD25, 1);
	SMAC_REG_WRITE(sh, ADR_PAD27, 8);
	SMAC_REG_WRITE(sh, ADR_PAD28, 8);
#endif
	ret = SMAC_LOAD_FW(sh);
    SMAC_REG_SET_BITS(sh, ADR_PHY_EN_1, (1 << RG_PHY_MD_EN_SFT), RG_PHY_MD_EN_MSK);

    SMAC_REG_READ(sh, FW_VERSION_REG, &regval);
    printk(KERN_INFO "Firmware version %d\n", regval);
exit:
	return ret;
}
void ssv6xxx_deinit_mac(struct ssv_softc *sc)
{
#ifdef SSV6200_ECO
	int i;
	for (i = 0; i < SSV_RC_MAX_STA; i++) {
		if (sc->sh->hw_buf_ptr[i])
			ssv6xxx_pbuf_free(sc, sc->sh->hw_buf_ptr[i]);
	}
#else
	if (sc->sh->hw_buf_ptr)
		ssv6xxx_pbuf_free(sc, sc->sh->hw_buf_ptr);
#endif
}
void inline ssv6xxx_deinit_hw(struct ssv_softc *sc)
{
	printk("%s(): \n", __FUNCTION__);
	ssv6xxx_deinit_mac(sc);
}
void ssv6xxx_restart_hw(struct ssv_softc *sc)
{
	printk("%s(): \n", __FUNCTION__);
	printk("**************************\n");
	printk("*** Software MAC reset ***\n");
	printk("**************************\n");
	sc->restart_counter++;
	sc->force_triger_reset = true;
	HCI_STOP(sc->sh);
	SMAC_REG_WRITE(sc->sh, 0xce000004, 0x0);
	sc->beacon_info[0].pubf_addr = 0x00;
	sc->beacon_info[1].pubf_addr = 0x00;
	ieee80211_restart_hw(sc->hw);
}
#ifdef CONFIG_SSV_CABRIO_E
extern struct ssv6xxx_iqk_cfg init_iqk_cfg;
#endif
static int ssv6xxx_init_hw(struct ssv_hw *sh)
{
    int ret=0;
#ifdef CONFIG_SSV_CABRIO_E
    u32 i, regval;
#endif
    sh->tx_desc_len = SSV6XXX_TX_DESC_LEN;
    sh->rx_desc_len = SSV6XXX_RX_DESC_LEN;
    sh->rx_pinfo_pad = 0x04;

    sh->tx_page_available = SSV6200_PAGE_TX_THRESHOLD;
    memset(sh->page_count, 0, sizeof(sh->page_count));

    
#ifdef CONFIG_SSV_CABRIO_E

    if (sh->cfg.force_chip_identity)
    {
        printk("Force use external RF setting [%08x]\n",sh->cfg.force_chip_identity);
        sh->cfg.chip_identity = sh->cfg.force_chip_identity;
    }

    if(sh->cfg.chip_identity == SSV6051Z)
    {
        memcpy(phy_info_tbl,phy_info_6051z,sizeof(phy_info_6051z));
    }

    switch (sh->cfg.chip_identity) {
        case SSV6051Q_P1:
        case SSV6051Q_P2:
        case SSV6051Q:
            printk("SSV6051Q setting\n");
            for (i=0; i<sizeof(ssv6200_rf_tbl)/sizeof(struct ssv6xxx_dev_table); i++){
                if (ssv6200_rf_tbl[i].address == 0xCE010008)
                    ssv6200_rf_tbl[i].data = 0x008DF61B;
                if (ssv6200_rf_tbl[i].address == 0xCE010014)
                    ssv6200_rf_tbl[i].data = 0x3D3E84FE;
                if (ssv6200_rf_tbl[i].address == 0xCE010018)
                    ssv6200_rf_tbl[i].data = 0x01457D79;
                if (ssv6200_rf_tbl[i].address == 0xCE01001C)
                    ssv6200_rf_tbl[i].data = 0x000103A7;
                if (ssv6200_rf_tbl[i].address == 0xCE010020)
                    ssv6200_rf_tbl[i].data = 0x000103A6;
                if (ssv6200_rf_tbl[i].address == 0xCE01002C)
                    ssv6200_rf_tbl[i].data = 0x00032CA8;
                if (ssv6200_rf_tbl[i].address == 0xCE010048)
                    ssv6200_rf_tbl[i].data = 0xFCCCCF27;
                if (ssv6200_rf_tbl[i].address == 0xCE010050)
                    ssv6200_rf_tbl[i].data = 0x0047C000;
            }
            break;
        case SSV6051Z:
            printk("SSV6051Z setting\n");
            for (i=0; i<sizeof(ssv6200_rf_tbl)/sizeof(struct ssv6xxx_dev_table); i++){
                if (ssv6200_rf_tbl[i].address == 0xCE010008)
                    ssv6200_rf_tbl[i].data = 0x004D561C;
                if (ssv6200_rf_tbl[i].address == 0xCE010014)
                    ssv6200_rf_tbl[i].data = 0x3D9E84FE;
                if (ssv6200_rf_tbl[i].address == 0xCE010018)
                    ssv6200_rf_tbl[i].data = 0x00457D79;
                if (ssv6200_rf_tbl[i].address == 0xCE01001C)
                    ssv6200_rf_tbl[i].data = 0x000103EB;
                if (ssv6200_rf_tbl[i].address == 0xCE010020)
                    ssv6200_rf_tbl[i].data = 0x000103EA;
                if (ssv6200_rf_tbl[i].address == 0xCE01002C)
                    ssv6200_rf_tbl[i].data = 0x00062CA8;
                if (ssv6200_rf_tbl[i].address == 0xCE010048)
                    ssv6200_rf_tbl[i].data = 0xFCCCCF27;
                if (ssv6200_rf_tbl[i].address == 0xCE010050)
                    ssv6200_rf_tbl[i].data = 0x0047C000;
            }
            break;
        case SSV6051P:
            printk("SSV6051P setting\n");
            for (i=0; i<sizeof(ssv6200_rf_tbl)/sizeof(struct ssv6xxx_dev_table); i++){
                if (ssv6200_rf_tbl[i].address == 0xCE010008)
                    ssv6200_rf_tbl[i].data = 0x008B7C1C;
                if (ssv6200_rf_tbl[i].address == 0xCE010014)
                    ssv6200_rf_tbl[i].data = 0x3D7E84FE;
                if (ssv6200_rf_tbl[i].address == 0xCE010018)
                    ssv6200_rf_tbl[i].data = 0x01457D79;
                if (ssv6200_rf_tbl[i].address == 0xCE01001C)
                    ssv6200_rf_tbl[i].data = 0x000103EB;
                if (ssv6200_rf_tbl[i].address == 0xCE010020)
                    ssv6200_rf_tbl[i].data = 0x000103EA;
                if (ssv6200_rf_tbl[i].address == 0xCE01002C)
                    ssv6200_rf_tbl[i].data = 0x00032CA8;
                if (ssv6200_rf_tbl[i].address == 0xCE010048)
                    ssv6200_rf_tbl[i].data = 0xFCCCCC27;
                if (ssv6200_rf_tbl[i].address == 0xCE010050)
                    ssv6200_rf_tbl[i].data = 0x0047C000;

                if (ssv6200_rf_tbl[i].address == 0xC0001D00)
                    ssv6200_rf_tbl[i].data = 0x5F000040;
            }

            break;
        default:
            printk("No RF setting\n");
            printk("Use default setting [SSV6051Z]\n");
            printk("**************\n");
            printk("* Call Help! *\n");
            printk("**************\n");
            for (i=0; i<sizeof(ssv6200_rf_tbl)/sizeof(struct ssv6xxx_dev_table); i++){
                if (ssv6200_rf_tbl[i].address == 0xCE010008)
                    ssv6200_rf_tbl[i].data = 0x004D561C;
                if (ssv6200_rf_tbl[i].address == 0xCE010014)
                    ssv6200_rf_tbl[i].data = 0x3D9E84FE;
                if (ssv6200_rf_tbl[i].address == 0xCE010018)
                    ssv6200_rf_tbl[i].data = 0x00457D79;
                if (ssv6200_rf_tbl[i].address == 0xCE01001C)
                    ssv6200_rf_tbl[i].data = 0x000103EB;
                if (ssv6200_rf_tbl[i].address == 0xCE010020)
                    ssv6200_rf_tbl[i].data = 0x000103EA;
                if (ssv6200_rf_tbl[i].address == 0xCE01002C)
                    ssv6200_rf_tbl[i].data = 0x00062CA8;
                if (ssv6200_rf_tbl[i].address == 0xCE010048)
                    ssv6200_rf_tbl[i].data = 0xFCCCCF27;
                if (ssv6200_rf_tbl[i].address == 0xCE010050)
                    ssv6200_rf_tbl[i].data = 0x0047C000;
            }

            WARN_ON(1);
            break;
    }

    if(sh->cfg.crystal_type == SSV6XXX_IQK_CFG_XTAL_26M)
    {
        init_iqk_cfg.cfg_xtal = SSV6XXX_IQK_CFG_XTAL_26M;
        printk("SSV6XXX_IQK_CFG_XTAL_26M\n");
    }
    else if(sh->cfg.crystal_type == SSV6XXX_IQK_CFG_XTAL_40M)
    {
        init_iqk_cfg.cfg_xtal = SSV6XXX_IQK_CFG_XTAL_40M;
        printk("SSV6XXX_IQK_CFG_XTAL_40M\n");
    } 
    else if(sh->cfg.crystal_type == SSV6XXX_IQK_CFG_XTAL_24M)
    {
        printk("SSV6XXX_IQK_CFG_XTAL_24M\n");
        init_iqk_cfg.cfg_xtal = SSV6XXX_IQK_CFG_XTAL_24M;
    }
    else
    {
        printk("Illegal xtal setting \n");
        BUG_ON(1);
    }

    for(i=0; i<sizeof(ssv6200_rf_tbl)/sizeof(struct ssv6xxx_dev_table); i++)
    {
        if(ssv6200_rf_tbl[i].address == ADR_SYN_KVCO_XO_FINE_TUNE_CBANK)
        {
            if(sh->cfg.crystal_frequency_offset)
            {
                ssv6200_rf_tbl[i].data &= RG_XOSC_CBANK_XO_I_MSK;
                ssv6200_rf_tbl[i].data |= (sh->cfg.crystal_frequency_offset << RG_XOSC_CBANK_XO_SFT);
            }
        }
    }

    //Resetting TX power
    for (i=0; i<sizeof(phy_setting)/sizeof(struct ssv6xxx_dev_table); i++){
        if (phy_setting[i].address == ADR_TX_GAIN_FACTOR){
            switch (sh->cfg.chip_identity) {
                case SSV6051Q_P1:
                case SSV6051Q_P2:
                case SSV6051Q:
                    printk("SSV6051Q setting [0x5B606C72]\n");
                    phy_setting[i].data = 0x5B606C72;
                    break;
                case SSV6051Z:
                    printk("SSV6051Z setting [0x60606060]\n");
                    phy_setting[i].data = 0x60606060;
                    break;
                case SSV6051P:
                    printk("SSV6051P setting [0x6C726C72]\n");
                    phy_setting[i].data = 0x6C726C72;
                    break;
                default:
                    printk("Use default power setting\n");
                    break;
            }
            //External B setting
            if (sh->cfg.wifi_tx_gain_level_b){
                phy_setting[i].data &= 0xffff0000;
                phy_setting[i].data |= wifi_tx_gain[sh->cfg.wifi_tx_gain_level_b] & 0x0000ffff;
            }
            //External G/N setting
            if (sh->cfg.wifi_tx_gain_level_gn){
                phy_setting[i].data &= 0x0000ffff;
                phy_setting[i].data |= wifi_tx_gain[sh->cfg.wifi_tx_gain_level_gn] & 0xffff0000;
            }

            printk("TX power setting 0x%x\n",phy_setting[i].data);
            //Setting TX power for RF calibration
            init_iqk_cfg.cfg_def_tx_scale_11b = (phy_setting[i].data>>0) & 0xff;
            init_iqk_cfg.cfg_def_tx_scale_11b_p0d5 = (phy_setting[i].data>>8) & 0xff;
            init_iqk_cfg.cfg_def_tx_scale_11g = (phy_setting[i].data>>16) & 0xff;
            init_iqk_cfg.cfg_def_tx_scale_11g_p0d5 = (phy_setting[i].data>>24) & 0xff;
            break;
        }
    }

    //volt regulator
#if SSV_VOLT_REGULATOR == VOLT_LDO_REGULATOR
    printk("Volt regulator LDO\n");
    for(i=0; i<sizeof(ssv6200_rf_tbl)/sizeof(struct ssv6xxx_dev_table); i++)
    {
        //0xC0001D08
        if(ssv6200_rf_tbl[i].address == ADR_PMU_2)
        {
            ssv6200_rf_tbl[i].data &= 0xFFFFFFFE;
            ssv6200_rf_tbl[i].data |= 0x00000000;
        }
    }
#elif SSV_VOLT_REGULATOR == VOLT_DCDC_CONVERT
    printk("Volt regulator DCDC\n");
    for(i=0; i<sizeof(ssv6200_rf_tbl)/sizeof(struct ssv6xxx_dev_table); i++)
    {
        //0xC0001D08
        if(ssv6200_rf_tbl[i].address == ADR_PMU_2)
        {
            ssv6200_rf_tbl[i].data &= 0xFFFFFFFE;
            ssv6200_rf_tbl[i].data |= 0x00000001;
        }
    }
#else
# error "Please redefine SSV_VOLT_REGULATOR!!"
#endif



#endif // CONFIG_SSV_CABRIO_E

    if (ret == 0) ret = SSV6XXX_SET_HW_TABLE(sh, ssv6200_rf_tbl);

    if (ret == 0) ret = SMAC_REG_WRITE(sh, 0xce000004, 0x00000000); /* ???? */

#ifdef CONFIG_SSV_CABRIO_E
        SMAC_REG_READ(sh, ADR_PHY_EN_0, &regval);
        if (regval & (1<<RG_RF_BB_CLK_SEL_SFT)) {
            printk("already do clock switch\n");
        }    
        else {
            printk("reset PLL\n");
            SMAC_REG_READ(sh, ADR_DPLL_CP_PFD_REGISTER, &regval);
            regval |= ((1<<RG_DP_BBPLL_PD_SFT) | (1<<RG_DP_BBPLL_SDM_EDGE_SFT));
            SMAC_REG_WRITE(sh, ADR_DPLL_CP_PFD_REGISTER, regval);
            regval &= ~((1<<RG_DP_BBPLL_PD_SFT) | (1<<RG_DP_BBPLL_SDM_EDGE_SFT));
            SMAC_REG_WRITE(sh, ADR_DPLL_CP_PFD_REGISTER, regval);
            mdelay(10);    
        }   
#endif

//-------------------------------------------------------------
    if (ret == 0) ret = SSV6XXX_SET_HW_TABLE(sh, ssv6200_phy_tbl);

//-------------------------------------------------------------

#ifdef CONFIG_SSV_CABRIO_E
    if (ret == 0) ret = SMAC_REG_WRITE(sh, ADR_TRX_DUMMY_REGISTER, 0xEAAAAAAA);
    SMAC_REG_READ(sh,ADR_TRX_DUMMY_REGISTER,&regval);
    if (regval != 0xEAAAAAAA)
    {
        printk("@@@@@@@@@@@@\n");
        printk(" SDIO issue -- please check 0xCE01008C %08x!!\n",regval);
        printk(" It shouble be 0xEAAAAAAA!!\n");
        printk("@@@@@@@@@@@@ \n");
    }
#endif

//-------------------------------------------------------------

#ifdef CONFIG_SSV_CABRIO_E                  
    if (ret == 0) ret = SMAC_REG_WRITE(sh, ADR_PAD53, 0x21);
    if (ret == 0) ret = SMAC_REG_WRITE(sh, ADR_PAD54, 0x3000);
    if (ret == 0) ret = SMAC_REG_WRITE(sh, ADR_PIN_SEL_0, 0x4000);

    if (ret == 0) ret = SMAC_REG_WRITE(sh, 0xc0000304, 0x01);
    if (ret == 0) ret = SMAC_REG_WRITE(sh, 0xc0000308, 0x01);
#endif // CONFIG_SSV_CABRIO_E

//-------------------------------------------------------------

    if (ret == 0) ret = SMAC_REG_WRITE(sh, ADR_CLOCK_SELECTION, 0x3);

//---------------------------------------------------------------------------------------------------------
#ifdef CONFIG_SSV_CABRIO_E
    if (ret == 0) ret = SMAC_REG_WRITE(sh, ADR_TRX_DUMMY_REGISTER, 0xAAAAAAAA);
#endif
//----------------------------------------------------------------------------------------------------------
	SMAC_REG_WRITE(sh, ADR_BRG_SW_RST, 1 << 1); 

    if ((ret=ssv6xxx_set_channel(sh->sc, sh->cfg.def_chan)))
        return ret;

    if (ret == 0) ret = SMAC_REG_WRITE(sh, 0xce000004, 0x17f); // B/G/N only
//-------------------------------------------------------------

    return ret;
}

static void ssv6xxx_read_configuration(struct ssv_hw *sh)
{
	if (is_valid_ether_addr(&ssv_cfg.maddr[0][0]))
		memcpy(&sh->cfg.maddr[0][0], &ssv_cfg.maddr[0][0], 6);
	if (is_valid_ether_addr(&ssv_cfg.maddr[1][0]))
		memcpy(&sh->cfg.maddr[1][0], &ssv_cfg.maddr[1][0], 6);
	if (ssv_cfg.hw_caps)
		sh->cfg.hw_caps = ssv_cfg.hw_caps;
	else
		sh->cfg.hw_caps = SSV6200_HW_CAP_HT | SSV6200_HW_CAP_2GHZ |
				  SSV6200_HW_CAP_SECURITY | SSV6200_HW_CAP_P2P |
				  SSV6200_HT_CAP_SGI_20 |
				  SSV6200_HW_CAP_AMPDU_RX |
				  SSV6200_HW_CAP_AMPDU_TX | SSV6200_HW_CAP_AP;
	if (ssv_cfg.def_chan)
		sh->cfg.def_chan = ssv_cfg.def_chan;
	else
		sh->cfg.def_chan = 6;
	sh->cfg.use_wpa2_only = ssv_cfg.use_wpa2_only;
#if SSV_XTAL == 26
	sh->cfg.crystal_type = SSV6XXX_IQK_CFG_XTAL_26M;
#elif SSV_XTAL == 40
	sh->cfg.crystal_type = SSV6XXX_IQK_CFG_XTAL_40M;
#elif SSV_XTAL == 24
	sh->cfg.crystal_type = SSV6XXX_IQK_CFG_XTAL_24M;
#else
#error "Please redefine SSV_XTAL!!"
#endif
	sh->cfg.wifi_tx_gain_level_gn = ssv_cfg.wifi_tx_gain_level_gn;
	sh->cfg.wifi_tx_gain_level_b = ssv_cfg.wifi_tx_gain_level_b;
    
    sh->cfg.force_chip_identity = ssv_cfg.force_chip_identity;
}
static int ssv6xxx_read_hw_info(struct ssv_softc *sc)
{
	struct ssv_hw *sh;
	sh = kzalloc(sizeof(struct ssv_hw), GFP_KERNEL);
	if (sh == NULL)
		return -ENOMEM;
	memset((void *)sh, 0, sizeof(struct ssv_hw));
	sc->sh = sh;
	sh->sc = sc;
	sh->priv = sc->dev->platform_data;
	ssv6xxx_read_configuration(sh);
	sh->hci.dev = sc->dev;
	sh->hci.hci_ops = NULL;
	sh->hci.hci_rx_cb = ssv6200_rx;
	sh->hci.rx_cb_args = (void *)sc;
	sh->hci.hci_tx_cb = ssv6xxx_tx_cb;
	sh->hci.tx_cb_args = (void *)sc;
#ifdef RATE_CONTROL_REALTIME_UPDATA
	sh->hci.hci_skb_update_cb = ssv6xxx_tx_rate_update;
	sh->hci.skb_update_args = (void *)sc;
#else
	sh->hci.hci_skb_update_cb = NULL;
	sh->hci.skb_update_args = NULL;
#endif
	sh->hci.hci_tx_flow_ctrl_cb = ssv6200_tx_flow_control;
	sh->hci.tx_fctrl_cb_args = (void *)sc;
	sh->hci.hci_tx_q_empty_cb = ssv6xxx_tx_q_empty_cb;
	sh->hci.tx_q_empty_args = (void *)sc;
	sh->hci.if_ops = sh->priv->ops;
	sh->hci.hci_tx_buf_free_cb = ssv6xxx_txbuf_free_skb;
	sh->hci.tx_buf_free_args = (void *)sc;
	return 0;
}
static int ssv6xxx_init_device(struct ssv_softc *sc, const char *name)
{
	struct ieee80211_hw *hw = sc->hw;
	struct ssv_hw *sh;
	int error = 0;
	BUG_ON(!sc->dev->platform_data);
	if ((error = ssv6xxx_read_hw_info(sc)) != 0) {
		return error;
	}
	sh = sc->sh;
	if (sh->cfg.hw_caps == 0)
		return -1;
	ssv6xxx_hci_register(&sh->hci);
	efuse_read_all_map(sh);
	if ((error = ssv6xxx_init_softc(sc)) != 0) {
		kfree(sh);
		return error;
	}
	if ((error = ssv6xxx_init_hw(sc->sh)) != 0) {
		ssv6xxx_deinit_softc(sc);
		return error;
	}
	if ((error = ieee80211_register_hw(hw)) != 0) {
		printk(KERN_ERR "Failed to register w. %d.\n", error);
		ssv6xxx_hci_deregister();
		ssv6xxx_deinit_softc(sc);
		return error;
	}
#ifdef CONFIG_SSV6XXX_DEBUGFS
	ssv6xxx_init_debugfs(sc, name);
#endif
	return 0;
}
static void ssv6xxx_deinit_device(struct ssv_softc *sc)
{
	printk("%s(): \n", __FUNCTION__);
#ifdef CONFIG_SSV6XXX_DEBUGFS
	ssv6xxx_deinit_debugfs(sc);
#endif
	ssv6xxx_rf_disable(sc->sh);
	ieee80211_unregister_hw(sc->hw);
	ssv6xxx_deinit_hw(sc);
	ssv6xxx_deinit_softc(sc);
	ssv6xxx_hci_deregister();
	kfree(sc->sh);
}
extern struct ieee80211_ops ssv6200_ops;
int ssv6xxx_dev_probe(struct platform_device *pdev)
{
#ifdef CONFIG_SSV6200_CLI_ENABLE
	extern struct ssv_softc *ssv_dbg_sc;
#endif
	struct ssv_softc *softc;
	struct ieee80211_hw *hw;
	int ret;
	if (!pdev->dev.platform_data) {
		dev_err(&pdev->dev, "no platform data specified!\n");
		return -EINVAL;
	}
	printk("%s(): ssv6200 device found !\n", __FUNCTION__);
	hw = ieee80211_alloc_hw(sizeof(struct ssv_softc), &ssv6200_ops);
	if (hw == NULL) {
		dev_err(&pdev->dev, "No memory for ieee80211_hw\n");
		return -ENOMEM;
	}
	SET_IEEE80211_DEV(hw, &pdev->dev);
	dev_set_drvdata(&pdev->dev, hw);
	memset((void *)hw->priv, 0, sizeof(struct ssv_softc));
	softc = hw->priv;
	softc->hw = hw;
	softc->dev = &pdev->dev;
	ret = ssv6xxx_init_device(softc, pdev->name);
	if (ret) {
		dev_err(&pdev->dev, "Failed to initialize device\n");
		ieee80211_free_hw(hw);
		return ret;
	}
#ifdef CONFIG_SSV6200_CLI_ENABLE
	ssv_dbg_sc = softc;
#endif
	wiphy_info(hw->wiphy, "%s\n", "SSV6200 of South Silicon Valley");
	return 0;
}
EXPORT_SYMBOL(ssv6xxx_dev_probe);
int ssv6xxx_dev_remove(struct platform_device *pdev)
{
	struct ieee80211_hw *hw = dev_get_drvdata(&pdev->dev);
	struct ssv_softc *softc = hw->priv;
	printk("ssv6xxx_dev_remove(): pdev=%p, hw=%p\n", pdev, hw);
	ssv6xxx_deinit_device(softc);
	printk("ieee80211_free_hw(): \n");
	ieee80211_free_hw(hw);
	pr_info("ssv6200: Driver unloaded\n");
	return 0;
}
EXPORT_SYMBOL(ssv6xxx_dev_remove);
static const struct platform_device_id ssv6xxx_id_table[] = {
	{ .name = "ssv6200", .driver_data = 0x00, }, {},
};
MODULE_DEVICE_TABLE(platform, ssv6xxx_id_table);
static struct platform_driver ssv6xxx_driver = {
	.probe = ssv6xxx_dev_probe,
#if LINUX_VERSION_CODE < KERNEL_VERSION(3, 7, 0)
	.remove = __devexit_p(ssv6xxx_dev_remove),
#else
	.remove = ssv6xxx_dev_remove,
#endif
	.id_table = ssv6xxx_id_table,
	.driver = { .name = "SSV WLAN driver", .owner = THIS_MODULE, }
};
#if (defined(CONFIG_SSV_SUPPORT_ANDROID) || defined(CONFIG_SSV_BUILD_AS_ONE_KO))
int ssv6xxx_init(void)
#else
static int __init ssv6xxx_init(void)
#endif
{
	extern void *ssv_dbg_phy_table;
	extern u32 ssv_dbg_phy_len;
	extern void *ssv_dbg_rf_table;
	extern u32 ssv_dbg_rf_len;
	ssv_dbg_phy_table = (void *)ssv6200_phy_tbl;
	ssv_dbg_phy_len =
	    sizeof(ssv6200_phy_tbl) / sizeof(struct ssv6xxx_dev_table);
	ssv_dbg_rf_table = (void *)ssv6200_rf_tbl;
	ssv_dbg_rf_len =
	    sizeof(ssv6200_rf_tbl) / sizeof(struct ssv6xxx_dev_table);
	return platform_driver_register(&ssv6xxx_driver);
}
#if (defined(CONFIG_SSV_SUPPORT_ANDROID) || defined(CONFIG_SSV_BUILD_AS_ONE_KO))
void ssv6xxx_exit(void)
#else
static void __exit ssv6xxx_exit(void)
#endif
{
	platform_driver_unregister(&ssv6xxx_driver);
}
#if (defined(CONFIG_SSV_SUPPORT_ANDROID) || defined(CONFIG_SSV_BUILD_AS_ONE_KO))
EXPORT_SYMBOL(ssv6xxx_init);
EXPORT_SYMBOL(ssv6xxx_exit);
#else
module_init(ssv6xxx_init);
module_exit(ssv6xxx_exit);
#endif
