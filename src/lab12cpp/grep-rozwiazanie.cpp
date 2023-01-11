#include <codecvt>
#include <fstream>
#include <future>
#include <iostream>
#include <list>
#include <locale>
#include <string>
#include <thread>
#include <vector>

int grep(std::string filename, std::wstring word) {
  std::wifstream file(filename);
  std::locale loc("pl_PL.UTF-8");
  file.imbue(loc);
  // Check for failbit now (e.g. if file doesn't exist).
  file.exceptions(std::wfstream::failbit);
  // Check only for badbit from now on.
  file.exceptions(std::wfstream::badbit);

  std::wstring line;
  int count = 0;
  while (getline(file, line)) {
    for (auto pos = line.find(word, 0); pos != std::string::npos;
         pos = line.find(word, pos + 1)) {
      count++;
    }
  }
  return count;
}

int main() {
  std::ios::sync_with_stdio(false);
  std::locale loc("pl_PL.UTF-8");
  std::wcout.imbue(loc);
  std::wcin.imbue(loc);
  std::wcout.exceptions(std::wfstream::badbit);
  std::wcin.exceptions(std::wfstream::badbit);

  std::wstring word;
  std::getline(std::wcin, word);

  std::wstring s_file_count;
  std::getline(std::wcin, s_file_count);
  int file_count = std::stoi(s_file_count);

  std::list<std::string> filenames{};

  std::wstring_convert<std::codecvt_utf8<wchar_t>, wchar_t> converter;

  for (int file_num = 0; file_num < file_count; file_num++) {
    std::wstring w_filename;
    std::getline(std::wcin, w_filename);
    std::string s_filename = converter.to_bytes(w_filename);
    filenames.push_back(s_filename);
  }

  constexpr size_t n_threads = 2;
  std::vector<std::vector<std::string>> filenames_by_thread(n_threads);
  size_t i = 0;
  for (auto filename : filenames) {
    filenames_by_thread[i].push_back(filename);
    i = (i + 1) % n_threads;
  }

  std::vector<std::thread> threads;
  std::vector<std::promise<int>> promises(n_threads);
  std::vector<std::future<int>> futures(n_threads);

  for (size_t i = 0; i < n_threads; i++) {
    auto &promise = promises[i];
    futures[i] = promise.get_future();
    auto &filenames_for_thread = filenames_by_thread[i];
    threads.emplace_back([&filenames_for_thread, &word, &promise]() {
      int count = 0;
      for (auto &filename : filenames_for_thread) {
          count += grep(filename, word);
      }
      promise.set_value(count);
    });
  }

  int count = 0;
  for (auto &future : futures) {
    count += future.get();
  }

  for (auto &thread : threads) {
    thread.join();
  }

  std::wcout << count << std::endl;
}
