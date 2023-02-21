#include <iostream>
#include <memory>
#include <mutex>
#include <thread>
#include <vector>
#include <future>

int sum;
std::mutex mtx;
void add(const std::vector<int> &v, int begin, int end) {
  int s = 0;
  for (int i = begin; i < end; i++) {
    s += v[i];
  }
  std::lock_guard<std::mutex> lock_guard(mtx);
  sum += s;
}

int main() {
  std::vector<int> v{1, 2, 3, 4, 5, 6, 7, 8, 9};
  std::vector<std::thread> threads;
  std::vector<std::future<int>> futures;
  for (int i = 0; i < v.size();) {
    int begin = i;
    int end = i + 3;
    threads.push_back(std::thread(add, std::ref(v), begin, end));
    i = end;
  }
  for (int i = 0; i < threads.size(); i++) {
    threads[i].join();
  }

  std::cout << "sum is "<< sum << std::endl;
}