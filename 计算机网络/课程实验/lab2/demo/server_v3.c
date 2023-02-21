/*
 * =====================================================================================
 *
 *    Filename:  server_v3.c
 *
 *
 *    Description:
 *
 *        Version:  1.0
 *       Revision:  none
 *       Compiler:  gcc
 *
 *
 *    Ref:
 *       https://gist.github.com/crouchggj/6894348
 *
 * =====================================================================================
 */

#include <arpa/inet.h>
#include <errno.h>
#include <fcntl.h>
#include <netinet/in.h>
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <unistd.h>

#define SERVPORT 3333
#define BACKLOG 10
#define MAX_CONNECTED_NO 10
#define MAXDATASIZE (5 * 1024)

struct arg_struct {
    int connect_fd;
    int arg2;


    
};

void handle_one_client(void *arg_struct) {
  int connect_fd = ((struct arg_struct*)arg_struct)->connect_fd;
  struct sockaddr_in connected_addr;
  int sin_size, recvbytes, sendbytes;
  char buf[MAXDATASIZE];

  struct sockaddr_in peer_addr;
  int len = sizeof(peer_addr);
  char peer_ip_addr[INET_ADDRSTRLEN]; //保存点分十进制的地址
  // inet_ntoa: 网络字节序IP转化点分十进制IP
  // ntohs: 将一个无符号长整形数从网络字节顺序转换为主机字节顺序

  getsockname(connect_fd, (struct sockaddr *)&connected_addr,
              &len); //获取connfd表示的连接上的本地地址
  printf("connected server address = %s:%d\n",
         inet_ntoa(connected_addr.sin_addr), ntohs(connected_addr.sin_port));
  getpeername(connect_fd, (struct sockaddr *)&peer_addr,
              &len); //获取connfd表示的连接上的对端地址
  printf("connected peer address = %s:%d\n",
         inet_ntop(AF_INET, &peer_addr.sin_addr, peer_ip_addr,
                   sizeof(peer_ip_addr)),
         ntohs(peer_addr.sin_port));

  sleep(3);
  if ((recvbytes = recv(connect_fd, buf, MAXDATASIZE, 0)) == -1) {
    perror("recv:");
    exit(1);
  }
  printf("Recv bytes: %d, buf: %s\n",recvbytes, buf);

  if ((sendbytes = send(connect_fd, buf, sizeof(buf), 0)) == -1) {
    perror("send:");
    exit(1);
  }
  printf("Send bytes: %d \n", sendbytes);
  close(connect_fd);
  return;
}

int main() {
  // sockaddr和sockaddr_in包含的数据都是一样的，但他们在使用上有区别：
  // 程序中不应操作sockaddr，sockaddr是给操作系统用的
  // 程序中应使用sockaddr_in来表示地址，sockaddr_in区分了地址和端口，使用更方便。
  struct sockaddr_in server_sockaddr, listen_sockaddr, connected_addr;
  int sin_size, recvbytes, sendbytes;
  int sockfd, connect_fd;
  char buf[MAXDATASIZE];
  // AF_INET: IPv4
  // SOCK_STREAM: tcp
  // IPPROTO_IP
  if ((sockfd = socket(AF_INET, SOCK_STREAM, 0)) == -1) {
    perror("socket:");
    exit(1);
  }

  int on;
  on = 1;
  setsockopt(
      sockfd, SOL_SOCKET, SO_REUSEADDR, &on,
      sizeof(
          on)); // 端口复用，当关闭程序后又重新开启，绑定同一个端口号会出错，因为socket关闭后释放端口需要时间。

  // https://www.gta.ufrj.br/ensino/eel878/sockets/sockaddr_inman.html
  server_sockaddr.sin_family = AF_INET;
  server_sockaddr.sin_port = htons(
      SERVPORT); // 将一个无符号短整型数值转换为网络字节序，即大端模式(big-endian)
  // INADDR_ANY is used when you don't need to bind a socket to a specific IP.
  // When you use this value as the address when calling bind(), the socket
  // accepts connections to all the IPs of the machine.
  server_sockaddr.sin_addr.s_addr = INADDR_ANY;
  memset(&(server_sockaddr.sin_zero), 0, 8);

  if ((bind(sockfd, (struct sockaddr *)&server_sockaddr,
            sizeof(struct sockaddr))) == -1) {
    perror("bind:");
    exit(1);
  }

  if (listen(sockfd, BACKLOG) == -1) {
    perror("listen:");
    exit(1);
  }
  printf("Start listen.....\n");
  sin_size = sizeof(listen_sockaddr);
  getsockname(sockfd, (struct sockaddr *)&listen_sockaddr,
              &sin_size); //获取监听的地址和端口
  printf("listen address = %s:%d\n", inet_ntoa(listen_sockaddr.sin_addr),
         ntohs(listen_sockaddr.sin_port));

  pthread_t thread_ids[1024];
  int thread_idx = -1;
  while (1) {
    sin_size = sizeof(struct sockaddr);
    if ((connect_fd = accept(sockfd, (struct sockaddr *)&connected_addr,
                             &sin_size)) == -1) {
      perror("accept:");
      exit(1);
    }
    pthread_t thread_id;
    struct arg_struct args;
    args.connect_fd = connect_fd;
    pthread_create(&thread_id, NULL, (void*)&handle_one_client, (void*)(&args));
    thread_idx++;
    thread_ids[thread_idx] = thread_id;
    if (thread_idx >= 1) {
      break;
    }
  }
  for(int i=0; i<=thread_idx; i++){
    pthread_join(thread_ids[i], NULL);
  }
  close(sockfd);
}



