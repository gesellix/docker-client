package de.gesellix.docker.explore

import java.lang.management.ManagementFactory
import java.lang.management.ThreadInfo
import java.lang.management.ThreadMXBean

class ThreadDumpHook extends Thread {

  void run() {
    dumpThreads()
  }

  static dumpThreads() {
    final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean()
    final ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds(), 100)
    for (ThreadInfo threadInfo : threadInfos) {
      System.out.println(threadInfo.getThreadName())
      final Thread.State state = threadInfo.getThreadState()
      System.out.println("   java.lang.Thread.State: " + state)
      final StackTraceElement[] stackTraceElements = threadInfo.getStackTrace()
      for (final StackTraceElement stackTraceElement : stackTraceElements) {
        System.out.println("        at " + stackTraceElement)
      }
      System.out.println("\n")
    }
  }
}
