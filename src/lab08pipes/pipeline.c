#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>

#include "err.h"
#include "pipeline-utils.h"

const char message[] = "Hello from your parent!";

void fprintarr(FILE *stream, char *prefix, char **arr) {
  fprintf(stream, "%s[", prefix);
  for (int i = 0; arr[i] != NULL; ++i) {
    if (i > 0)
      fprintf(stream, ", ");
    fprintf(stream, "%s", arr[i]);
  }
  fprintf(stream, "]\n");
}

int main(int argc, char *argv[]) {
  assert(argv[argc] == NULL);

  char *new_argv[argc];
  int new_argc = argc - 1;
  new_argv[0] = argv[0];
  for (int i = 2; i < argc; ++i)
    new_argv[i - 1] = argv[i];
  new_argv[argc - 1] = NULL;

  fprintf(stderr, "#%6i: ", getpid());
  fprintarr(stderr, "", argv);
  fprintarr(stderr, "  new_argv = ", new_argv);

  if (argc == 2) {
    fprintf(stderr, "#%6i: execl(%s, %s)\n", getpid(), argv[1], argv[1]);
    ASSERT_SYS_OK(execlp(argv[1], argv[1], NULL));
  }
  if (argc < 2) {
    fprintf(stderr, "#%6i: too few arguments\n", getpid());
    print_open_descriptors();
    exit(-1);
  }

  int pipe_dsc[2];
  ASSERT_SYS_OK(pipe(pipe_dsc));

  usleep(1000);
  pid_t pid = fork();
  ASSERT_SYS_OK(pid);
  if (!pid) { // Child process.
    fprintf(stderr, "fork(#%i) -> #%i\n", getppid(), getpid());
    // Close writing end of pipe.
    ASSERT_SYS_OK(close(pipe_dsc[1]));

    // Replace the standard input with the pipe's reading end.
    fprintf(stderr, "Child: replacing stdin descriptor %d with a copy of %d\n",
            STDIN_FILENO, pipe_dsc[0]);
    ASSERT_SYS_OK(dup2(pipe_dsc[0], STDIN_FILENO));
    ASSERT_SYS_OK(close(pipe_dsc[0])); // Close the original copy.

    print_open_descriptors();
    ASSERT_SYS_OK(execvp("./pipeline", new_argv));
  } else { // Parent process.
    usleep(1000);
    fprintf(stderr, "fork(#%i) -> parent #%i\n", getppid(), getpid());

    // Close reading end of pipe.
    ASSERT_SYS_OK(close(pipe_dsc[0]));

    fprintf(stderr, "Child: replacing stdout descriptor %d with a copy of %d\n",
            STDIN_FILENO, pipe_dsc[0]);
    ASSERT_SYS_OK(dup2(pipe_dsc[1], STDOUT_FILENO));
    ASSERT_SYS_OK(close(pipe_dsc[1])); // Close the original copy.

    fprintf(stderr, "#%6i: execl(%s, %s)\n", getpid(), argv[1], argv[1]);
    print_open_descriptors();
    ASSERT_SYS_OK(execlp(argv[1], argv[1], NULL));

    return 0;
  }
}
