/*
 * Copyright (c) 2023 looly(loolly@aliyun.com)
 * Hutool is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package org.dromara.hutool.cron;

import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.cron.pattern.CronPattern;
import org.dromara.hutool.cron.task.CronTask;
import org.dromara.hutool.cron.task.Task;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 定时任务表<br>
 * 任务表将ID、表达式、任务一一对应，定时任务执行过程中，会周期性检查定时任务表中的所有任务表达式匹配情况，从而执行其对应的任务<br>
 * 任务的添加、移除使用读写锁保证线程安全性
 *
 * @author Looly
 */
public class TaskTable implements Serializable {
	private static final long serialVersionUID = 1L;

	public static final int DEFAULT_CAPACITY = 10;

	private final ReadWriteLock lock;

	private final List<String> ids;
	private final List<CronPattern> patterns;
	private final List<Task> tasks;
	private int size;

	/**
	 * 构造
	 */
	public TaskTable() {
		this(DEFAULT_CAPACITY);
	}

	/**
	 * 构造
	 *
	 * @param initialCapacity 容量，即预估的最大任务数
	 */
	public TaskTable(final int initialCapacity) {
		lock = new ReentrantReadWriteLock();

		ids = new ArrayList<>(initialCapacity);
		patterns = new ArrayList<>(initialCapacity);
		tasks = new ArrayList<>(initialCapacity);
	}

	/**
	 * 新增Task
	 *
	 * @param id      ID
	 * @param pattern {@link CronPattern}
	 * @param task    {@link Task}
	 * @return this
	 */
	public TaskTable add(final String id, final CronPattern pattern, final Task task) {
		final Lock writeLock = lock.writeLock();
		writeLock.lock();
		try {
			if (ids.contains(id)) {
				throw new CronException("Id [{}] has been existed!", id);
			}
			ids.add(id);
			patterns.add(pattern);
			tasks.add(task);
			size++;
		} finally {
			writeLock.unlock();
		}
		return this;
	}

	/**
	 * 获取所有ID，返回不可变列表，即列表不可修改
	 *
	 * @return ID列表
	 * @since 4.6.7
	 */
	public List<String> getIds() {
		final Lock readLock = lock.readLock();
		readLock.lock();
		try {
			return Collections.unmodifiableList(this.ids);
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * 获取所有定时任务表达式，返回不可变列表，即列表不可修改
	 *
	 * @return 定时任务表达式列表
	 * @since 4.6.7
	 */
	public List<CronPattern> getPatterns() {
		final Lock readLock = lock.readLock();
		readLock.lock();
		try {
			return Collections.unmodifiableList(this.patterns);
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * 获取所有定时任务，返回不可变列表，即列表不可修改
	 *
	 * @return 定时任务列表
	 * @since 4.6.7
	 */
	public List<Task> getTasks() {
		final Lock readLock = lock.readLock();
		readLock.lock();
		try {
			return Collections.unmodifiableList(this.tasks);
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * 移除Task
	 *
	 * @param id Task的ID
	 * @return 是否成功移除，{@code false}表示未找到对应ID的任务
	 */
	public boolean remove(final String id) {
		final Lock writeLock = lock.writeLock();
		writeLock.lock();
		try {
			final int index = ids.indexOf(id);
			if (index < 0) {
				return false;
			}
			tasks.remove(index);
			patterns.remove(index);
			ids.remove(index);
			size--;
		} finally {
			writeLock.unlock();
		}
		return true;
	}

	/**
	 * 更新某个Task的定时规则
	 *
	 * @param id      Task的ID
	 * @param pattern 新的表达式
	 * @return 是否更新成功，如果id对应的规则不存在则不更新
	 * @since 4.0.10
	 */
	public boolean updatePattern(final String id, final CronPattern pattern) {
		final Lock writeLock = lock.writeLock();
		writeLock.lock();
		try {
			final int index = ids.indexOf(id);
			if (index > -1) {
				patterns.set(index, pattern);
				return true;
			}
		} finally {
			writeLock.unlock();
		}
		return false;
	}

	/**
	 * 获得指定位置的{@link Task}
	 *
	 * @param index 位置
	 * @return {@link Task}
	 * @since 3.1.1
	 */
	public Task getTask(final int index) {
		final Lock readLock = lock.readLock();
		readLock.lock();
		try {
			return tasks.get(index);
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * 获得指定id的{@link Task}
	 *
	 * @param id ID
	 * @return {@link Task}
	 * @since 3.1.1
	 */
	public Task getTask(final String id) {
		final int index = ids.indexOf(id);
		if (index > -1) {
			return getTask(index);
		}
		return null;
	}

	/**
	 * 获得指定位置的{@link CronPattern}
	 *
	 * @param index 位置
	 * @return {@link CronPattern}
	 * @since 3.1.1
	 */
	public CronPattern getPattern(final int index) {
		final Lock readLock = lock.readLock();
		readLock.lock();
		try {
			return patterns.get(index);
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * 任务表大小，加入的任务数
	 *
	 * @return 任务表大小，加入的任务数
	 * @since 4.0.2
	 */
	public int size() {
		return this.size;
	}

	/**
	 * 任务表是否为空
	 *
	 * @return true为空
	 * @since 4.0.2
	 */
	public boolean isEmpty() {
		return this.size < 1;
	}

	/**
	 * 获得指定id的{@link CronPattern}
	 *
	 * @param id ID
	 * @return {@link CronPattern}
	 * @since 3.1.1
	 */
	public CronPattern getPattern(final String id) {
		final int index = ids.indexOf(id);
		if (index > -1) {
			return getPattern(index);
		}
		return null;
	}

	/**
	 * 如果时间匹配则执行相应的Task，带读锁
	 *
	 * @param scheduler {@link Scheduler}
	 * @param millis 时间毫秒
	 */
	public void executeTaskIfMatch(final Scheduler scheduler, final long millis) {
		final Lock readLock = lock.readLock();
		readLock.lock();
		try {
			executeTaskIfMatchInternal(scheduler, millis);
		} finally {
			readLock.unlock();
		}
	}

	@Override
	public String toString() {
		final StringBuilder builder = StrUtil.builder();
		for (int i = 0; i < size; i++) {
			builder.append(StrUtil.format("[{}] [{}] [{}]\n",
					ids.get(i), patterns.get(i), tasks.get(i)));
		}
		return builder.toString();
	}

	/**
	 * 如果时间匹配则执行相应的Task，无锁
	 *
	 * @param scheduler {@link Scheduler}
	 * @param millis 时间毫秒
	 * @since 3.1.1
	 */
	protected void executeTaskIfMatchInternal(final Scheduler scheduler, final long millis) {
		for (int i = 0; i < size; i++) {
			if (patterns.get(i).match(scheduler.config.timezone, millis, scheduler.config.matchSecond)) {
				scheduler.taskExecutorManager.spawnExecutor(new CronTask(ids.get(i), patterns.get(i), tasks.get(i)));
			}
		}
	}
}