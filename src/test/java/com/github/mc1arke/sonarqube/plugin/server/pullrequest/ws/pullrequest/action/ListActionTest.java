/*
 * Copyright (C) 2022-2024 Michael Clarke
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.pullrequest.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.DateUtils;
import org.sonar.db.DbClient;
import org.sonar.db.component.BranchDao;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.SnapshotDao;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.MeasureDao;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.protobuf.DbProjectBranches;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.ProjectPullRequests;

class ListActionTest {

    private final DbClient dbClient = mock();
    private final UserSession userSession = mock();
    private final ComponentFinder componentFinder = mock();
    private final ProtoBufWriter protoBufWriter = mock();
    private final ListAction underTest = new ListAction(dbClient, componentFinder, userSession, protoBufWriter);

    @Test
    void shouldDefineEndpointWithProjectParameter() {
        WebService.NewController newController = mock();
        WebService.NewAction newAction = mock();
        when(newAction.setHandler(any())).thenReturn(newAction);
        when(newController.createAction(any())).thenReturn(newAction);
        WebService.NewParam projectParam = mock();
        when(newAction.createParam(any())).thenReturn(projectParam);

        underTest.define(newController);

        verify(newController).createAction("list");
        verify(newAction).setHandler(underTest);
        verify(newAction).createParam("project");
        verifyNoMoreInteractions(newAction);
        verify(projectParam).setRequired(true);
        verifyNoMoreInteractions(projectParam);

        verifyNoMoreInteractions(newController);
    }

    @Test
    void shouldExecuteRequestWithValidParameter() {
        Request request = mock();
        when(request.mandatoryParam("project")).thenReturn("project");

        when(componentFinder.getProjectByKey(any(), any())).thenReturn(new ProjectDto().setKey("projectKey").setUuid("uuid0"));

        when(userSession.hasPermission(any())).thenReturn(true);

        BranchDao branchDao = mock();
        when(dbClient.branchDao()).thenReturn(branchDao);
        when(branchDao.selectByProject(any(), any())).thenReturn(List.of(new BranchDto()
            .setBranchType(BranchType.PULL_REQUEST)
            .setKey("prKey")
            .setUuid("uuid1")
            .setMergeBranchUuid("uuid2")
            .setPullRequestData(DbProjectBranches.PullRequestData.newBuilder()
                .setBranch("prBranch")
                .setTitle("title")
                .setTarget("target")
                .setUrl("url")
                .build()),
            new BranchDto()
                .setBranchType(BranchType.PULL_REQUEST)
                .setKey("prKey2")
                .setUuid("uuid3")
                .setMergeBranchUuid("orphan")
                .setPullRequestData(DbProjectBranches.PullRequestData.newBuilder()
                    .setBranch("prBranch2")
                    .setTitle("title2")
                    .setUrl("url2")
                    .build()),
            new BranchDto()
                .setBranchType(BranchType.PULL_REQUEST)
                .setKey("prKey3")
                .setUuid("uuid4")
                .setMergeBranchUuid("uuid2")
                .setPullRequestData(DbProjectBranches.PullRequestData.newBuilder()
                    .setBranch("prBranch2")
                    .setTitle("title3")
                    .setUrl("url3")
                    .build()),
            new BranchDto()
                .setBranchType(BranchType.PULL_REQUEST)
                .setKey("prKey4")
                .setUuid("uuid5")
                .setMergeBranchUuid("uuid2")));

        when(branchDao.selectByUuids(any(), any())).thenReturn(List.of(new BranchDto()
            .setUuid("uuid2")
            .setKey("branch2Key")));

        MeasureDao measureDao = mock();
        when(dbClient.measureDao()).thenReturn(measureDao);
        when(measureDao.selectByComponentUuidsAndMetricKeys(any(), any(), any())).thenReturn(List.of(new MeasureDto()
            .setComponentUuid("uuid1")
            .addValue(CoreMetrics.ALERT_STATUS_KEY, "live measure")));

        SnapshotDao snapshotDao = mock();
        when(dbClient.snapshotDao()).thenReturn(snapshotDao);
        when(snapshotDao.selectLastAnalysesByRootComponentUuids(any(), any())).thenReturn(List.of(new SnapshotDto().setRootComponentUuid("uuid3").setCreatedAt(1234567891234L)));

        Response response = mock();

        ProjectPullRequests.ListWsResponse expected = ProjectPullRequests.ListWsResponse.newBuilder()
            .addPullRequests(ProjectPullRequests.PullRequest.newBuilder()
                .setKey("prKey")
                .setTitle("title")
                .setBranch("prBranch")
                .setBase("branch2Key")
                .setStatus(ProjectPullRequests.Status.newBuilder()
                    .setQualityGateStatus("live measure")
                    .build())
                .setUrl("url")
                .setTarget("target")
                .build())
            .addPullRequests(ProjectPullRequests.PullRequest.newBuilder()
                .setKey("prKey2")
                .setTitle("title2")
                .setBranch("prBranch2")
                .setAnalysisDate(DateUtils.formatDateTime(1234567891234L))
                .setStatus(ProjectPullRequests.Status.newBuilder()
                    .build())
                .setIsOrphan(true)
                .setUrl("url2"))
            .addPullRequests(ProjectPullRequests.PullRequest.newBuilder()
                .setKey("prKey3")
                .setTitle("title3")
                .setBranch("prBranch2")
                .setBase("branch2Key")
                .setStatus(ProjectPullRequests.Status.newBuilder()
                    .build())
                .setUrl("url3")
                .setTarget("branch2Key")
                .build())
            .addPullRequests(ProjectPullRequests.PullRequest.newBuilder()
                .setKey("prKey4")
                .setBase("branch2Key")
                .setStatus(ProjectPullRequests.Status.newBuilder().build())
                .setTarget("branch2Key")
                .build())
            .build();


        underTest.handle(request, response);

        ArgumentCaptor<ProjectPullRequests.ListWsResponse> messageArgumentCaptor = ArgumentCaptor.captor();
        verify(protoBufWriter).write(messageArgumentCaptor.capture(), eq(request), eq(response));

        assertThat(messageArgumentCaptor.getValue()).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldNotExecuteRequestIfUserDoesNotHaveAnyPermissions() {
        Request request = mock();
        when(request.mandatoryParam("project")).thenReturn("project");

        Response response = mock();

        assertThatThrownBy(() -> underTest.handle(request, response)).isInstanceOf(ForbiddenException.class);

        verifyNoMoreInteractions(protoBufWriter);
    }

}