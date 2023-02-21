/*
 * =====================================================================================
 *
 *    Filename:  client.c
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

#include <error.h>
#include <fcntl.h>
#include <netdb.h>
#include <netinet/in.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <sys/types.h>
#include <unistd.h>

#define SEVPORT 3333
#define MAXDATASIZE (1024 * 5)

#define TIME_DIFF(t1, t2)                                                      \
  (((t1).tv_sec - (t2).tv_sec) * 1000 + ((t1).tv_usec - (t2).tv_usec) / 1000)

int main(int argc, char *argv[]) {
  int sockfd, sendbytes, recvbytes;
  char buf[MAXDATASIZE];
  struct hostent *host;
  struct sockaddr_in serv_addr;
  struct timeval timestamp;
  struct timeval timestamp_end;
  if (argc < 2) {
    fprintf(stderr, "Please enter the server's hostname!\n");
    exit(1);
  }

  if ((host = gethostbyname(argv[1])) == NULL) {
    perror("gethostbyname:");
    exit(1);
  }

  printf("hostent h_name: %s , h_aliases: %s,\
			h_addrtype: %d, h_length: %d, h_addr_list: %s\n",
         host->h_name, *(host->h_aliases), host->h_addrtype, host->h_length,
         *(host->h_addr_list));

  if ((sockfd = socket(AF_INET, SOCK_STREAM, 0)) == -1) {
    perror("socket:");
    exit(1);
  }

  serv_addr.sin_family = AF_INET;
  serv_addr.sin_port = htons(SEVPORT);
  serv_addr.sin_addr = *((struct in_addr *)host->h_addr);
  bzero(&(serv_addr.sin_zero), 8);

  if (connect(sockfd, (struct sockaddr *)&serv_addr, sizeof(struct sockaddr)) ==
      -1) {
    perror("connect:");
    exit(1);
  }
  printf("connect server success.\n");
  memset(buf, 0x00, sizeof(buf));
  strcpy(buf, "hello world!");
  gettimeofday(&timestamp, NULL);

  if ((sendbytes = send(sockfd, buf, sizeof(buf), 0)) == -1) {
    perror("send:");
    exit(1);
  }

  gettimeofday(&timestamp_end, NULL);
  printf("sendbytes: %d, cost time: %ld ms\n", sendbytes,
         TIME_DIFF(timestamp_end, timestamp));

  memset(buf, 0x00, sizeof(buf));
  if ((recvbytes = recv(sockfd, buf, MAXDATASIZE, 0)) == -1) {
    perror("recv");
    close(sockfd);
    exit(1);
  }
  printf("Client receive bytes: %d, msg: %s\n", recvbytes, buf);
  close(sockfd);
}