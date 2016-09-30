#include <linux/taskstats.h>
#include <netlink/socket.h>
#include <netlink/genl/ctrl.h>
#include <netlink/genl/genl.h>

nl_sock *nl;
int family_id = 0;

int open() {
    nl = nl_socket_alloc();
    genl_connect(nl);
    family_id = genl_ctrl_resolve(nl, TASKSTATS_GENL_NAME);
    return 0;
}

class TaskStatistics {
public:
  TaskStatistics(const taskstats&);
  TaskStatistics() = default;
  TaskStatistics(const TaskStatistics&) = default;
  void AddPidToTgid(const TaskStatistics&);
  TaskStatistics Update(const TaskStatistics&);

  pid_t pid() const { return pid_; }
  const std::string& comm() const { return comm_; }
  uint64_t read() const { return read_bytes_; }
  uint64_t write() const { return write_bytes_; }
  uint64_t read_write() const { return read_write_bytes_; }
  uint64_t delay_io() const { return block_io_delay_ns_; }
  uint64_t delay_swap() const { return swap_in_delay_ns_; }
  uint64_t delay_sched() const { return cpu_delay_ns_; }
  uint64_t delay_mem() const { return reclaim_delay_ns_; }
  uint64_t delay_total() const { return total_delay_ns_; }
  int threads() const { return threads_; }

  void set_pid(pid_t pid) { pid_ = pid; }

private:
  std::string comm_;
  uid_t uid_;
  gid_t gid_;
  pid_t pid_;
  pid_t ppid_;

  uint64_t cpu_delay_count_;
  uint64_t cpu_delay_ns_;

  uint64_t block_io_delay_count_;
  uint64_t block_io_delay_ns_;

  uint64_t swap_in_delay_count_;
  uint64_t swap_in_delay_ns_;

  uint64_t reclaim_delay_count_;
  uint64_t reclaim_delay_ns_;

  uint64_t total_delay_ns_;

  uint64_t cpu_time_real_;
  uint64_t cpu_time_virtual_;

  uint64_t read_bytes_;
  uint64_t write_bytes_;
  uint64_t read_write_bytes_;
  uint64_t cancelled_write_bytes_;

  int threads_;
};





static pid_t ParseAggregateTaskStats(nlattr* attr, int attr_size,
                                     taskstats* stats) {
  pid_t received_pid = -1;
  nla_for_each_attr(attr, attr, attr_size, attr_size) {
    switch (nla_type(attr)) {
    case TASKSTATS_TYPE_PID:
    case TASKSTATS_TYPE_TGID:
      received_pid = nla_get_u32(attr);
      break;
    case TASKSTATS_TYPE_STATS:
    {
      int len = static_cast<int>(sizeof(*stats));
      len = std::min(len, nla_len(attr));
      nla_memcpy(stats, attr, len);
      return received_pid;
    }
    default:
      LOG(ERROR) << "unexpected attribute inside AGGR";
      return -1;
    }
  }

  return -1;
}


static int ParseTaskStats(nl_msg* msg, void* arg) {
  genlmsghdr* gnlh = static_cast<genlmsghdr*>(nlmsg_data(nlmsg_hdr(msg)));
  nlattr* attr = genlmsg_attrdata(gnlh, 0);
  int remaining = genlmsg_attrlen(gnlh, 0);

  nla_for_each_attr(attr, attr, remaining, remaining) {
    switch (nla_type(attr)) {
    case TASKSTATS_TYPE_AGGR_PID:
    case TASKSTATS_TYPE_AGGR_TGID:
    {
      nlattr* nested_attr = static_cast<nlattr*>(nla_data(attr));
      taskstats stats;
      pid_t ret;

      ret = ParseAggregateTaskStats(nested_attr, nla_len(attr), &stats);
      if (ret < 0) {
        printf("error bad AGGR_PID\n");
      } else if (ret == pid) {
        //taskstats_request->stats = stats;
      } else {
        printf("error unexpected PID %d\n",ret);
      }
      break;
    }
    case TASKSTATS_TYPE_NULL:
      break;
    default:
      printf("unexpected attribute in taskstats");
    }
  }
  return NL_OK;
}

int getstat(int pid) {


  //TaskStatsRequest taskstats_request = TaskStatsRequest();
  //taskstats_request.requested_pid = pid;

  //std::unique_ptr<nl_msg, decltype(&nlmsg_free)> message(nlmsg_alloc(),
  //                                                       nlmsg_free);
  nl_msg * message = nlmsg_alloc();
  genlmsg_put(message, NL_AUTO_PID, NL_AUTO_SEQ, family_id, 0, 0,
              TASKSTATS_CMD_GET, TASKSTATS_VERSION);
  nla_put_u32(message, TASKSTATS_CMD_ATTR_TGID, pid);

  int result = nl_send_auto_complete(nl, message);
  printf("nl request result: %d\n",result);

//  std::unique_ptr<nl_cb, decltype(&nl_cb_put)> callbacks(
 //     nl_cb_alloc(NL_CB_DEFAULT), nl_cb_put);
  nl_cb* callbacks = nl_cb_alloc(NL_CB_DEFAULT);

  nl_cb_set(callbacks, NL_CB_VALID, NL_CB_CUSTOM, &ParseTaskStats,
            NULL);

  result = nl_recvmsgs(nl, callbacks);
  printf("nl request callback result: %d\n",result);
  nl_wait_for_ack(nl);

  stats = TaskStatistics(taskstats_request.stats);

  return true;
}

int main(int argc, char* []argv) {


    return 0;
}

