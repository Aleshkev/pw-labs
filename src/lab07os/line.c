#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>

#include "err.h"

#define N_PROC 5

int child(int index) {
  printf("Child, process #%d: my pid is %d, my parent's pid is %d\n", index,
         getpid(), getppid());

  char index_str[256];
  int ret = snprintf(index_str, sizeof index_str, "%d", index);
  if (ret < 0 || ret >= (int)sizeof(index_str)) fatal("snprintf failed");

  ASSERT_SYS_OK(execl("./line", "./line", index_str, NULL));
  return 0;
}

int main(int argc, char* argv[]) {
  assert(argc <= 2);

  int index = (argc == 1 ? N_PROC : atoi(argv[1]));
  
  if (index > 1) {
    pid_t child_pid;
    ASSERT_SYS_OK(child_pid = fork());
    if (child_pid == 0) return child(index - 1);
    ASSERT_SYS_OK(wait(NULL));
  }
}
