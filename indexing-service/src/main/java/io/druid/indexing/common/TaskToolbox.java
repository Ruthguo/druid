/*
 * Druid - a distributed column store.
 * Copyright (C) 2012, 2013  Metamarkets Group Inc.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package io.druid.indexing.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.metamx.emitter.service.ServiceEmitter;
import com.metamx.metrics.MonitorScheduler;
import io.druid.client.ServerView;
import io.druid.indexing.common.actions.TaskActionClient;
import io.druid.indexing.common.actions.TaskActionClientFactory;
import io.druid.indexing.common.config.TaskConfig;
import io.druid.indexing.common.task.Task;
import io.druid.query.QueryRunnerFactoryConglomerate;
import io.druid.segment.loading.DataSegmentKiller;
import io.druid.segment.loading.DataSegmentPusher;
import io.druid.segment.loading.MMappedQueryableIndexFactory;
import io.druid.segment.loading.S3DataSegmentPuller;
import io.druid.segment.loading.SegmentLoaderConfig;
import io.druid.segment.loading.SegmentLoadingException;
import io.druid.segment.loading.SingleSegmentLoader;
import io.druid.server.coordination.DataSegmentAnnouncer;
import io.druid.timeline.DataSegment;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Stuff that may be needed by a Task in order to conduct its business.
 */
public class TaskToolbox
{
  private final TaskConfig config;
  private final Task task;
  private final TaskActionClientFactory taskActionClientFactory;
  private final ServiceEmitter emitter;
  private final RestS3Service s3Client;
  private final DataSegmentPusher segmentPusher;
  private final DataSegmentKiller dataSegmentKiller;
  private final DataSegmentAnnouncer segmentAnnouncer;
  private final ServerView newSegmentServerView;
  private final QueryRunnerFactoryConglomerate queryRunnerFactoryConglomerate;
  private final MonitorScheduler monitorScheduler;
  private final ObjectMapper objectMapper;

  public TaskToolbox(
      TaskConfig config,
      Task task,
      TaskActionClientFactory taskActionClientFactory,
      ServiceEmitter emitter,
      RestS3Service s3Client,
      DataSegmentPusher segmentPusher,
      DataSegmentKiller dataSegmentKiller,
      DataSegmentAnnouncer segmentAnnouncer,
      ServerView newSegmentServerView,
      QueryRunnerFactoryConglomerate queryRunnerFactoryConglomerate,
      MonitorScheduler monitorScheduler,
      ObjectMapper objectMapper
  )
  {
    this.config = config;
    this.task = task;
    this.taskActionClientFactory = taskActionClientFactory;
    this.emitter = emitter;
    this.s3Client = s3Client;
    this.segmentPusher = segmentPusher;
    this.dataSegmentKiller = dataSegmentKiller;
    this.segmentAnnouncer = segmentAnnouncer;
    this.newSegmentServerView = newSegmentServerView;
    this.queryRunnerFactoryConglomerate = queryRunnerFactoryConglomerate;
    this.monitorScheduler = monitorScheduler;
    this.objectMapper = objectMapper;
  }

  public TaskConfig getConfig()
  {
    return config;
  }

  public TaskActionClient getTaskActionClient()
  {
    return taskActionClientFactory.create(task);
  }

  public ServiceEmitter getEmitter()
  {
    return emitter;
  }

  public DataSegmentPusher getSegmentPusher()
  {
    return segmentPusher;
  }

  public DataSegmentKiller getDataSegmentKiller()
  {
    return dataSegmentKiller;
  }

  public DataSegmentAnnouncer getSegmentAnnouncer()
  {
    return segmentAnnouncer;
  }

  public ServerView getNewSegmentServerView()
  {
    return newSegmentServerView;
  }

  public QueryRunnerFactoryConglomerate getQueryRunnerFactoryConglomerate()
  {
    return queryRunnerFactoryConglomerate;
  }

  public MonitorScheduler getMonitorScheduler()
  {
    return monitorScheduler;
  }

  public ObjectMapper getObjectMapper()
  {
    return objectMapper;
  }

  public Map<DataSegment, File> getSegments(List<DataSegment> segments)
      throws SegmentLoadingException
  {
    final SingleSegmentLoader loader = new SingleSegmentLoader(
        new S3DataSegmentPuller(s3Client),
        new MMappedQueryableIndexFactory(),
        new SegmentLoaderConfig()
        {
          @Override
          public String getLocations()
          {
            return new File(getTaskWorkDir(), "fetched_segments").toString();
          }
        }
    );

    Map<DataSegment, File> retVal = Maps.newLinkedHashMap();
    for (DataSegment segment : segments) {
      retVal.put(segment, loader.getSegmentFiles(segment));
    }

    return retVal;
  }

  public File getTaskWorkDir()
  {
    return new File(new File(config.getBaseTaskDir(), task.getId()), "work");
  }
}
