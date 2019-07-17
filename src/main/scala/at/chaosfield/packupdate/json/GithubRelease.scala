package at.chaosfield.packupdate.json

import java.util.Date

case class GithubRelease(
                          id: Int,
                          url: String,
                          assetsUrl: String,
                          htmlUrl: String,
                          nodeId: String,
                          tagName: String,
                          targetCommitish: String,
                          name: String,
                          draft: Boolean,
                          author: GithubUser,
                          prerelease: Boolean,
                          createdAt: Date,
                          publishedAt: Date,
                          assets: Array[GithubAsset],
                          tarballUrl: String,
                          zipballUrl: String,
                          body: String
                        )

case class GithubUser(
                      id: Int,
                      login: String,
                      nodeId: String,
                      avatarUrl: String,
                      gravatarId: String,
                      url: String,
                      htmlUrl: String,
                      followersUrl: String,
                      followingUrl: String,
                      gistsUrl: String,
                      starredUrl: String,
                      subscriptionsUrl: String,
                      organizationsUrl: String,
                      reposUrl: String,
                      eventsUrl: String,
                      receivedEventsUrl: String,
                      `type`: String,
                      siteAdmin: Boolean
                     )

case class GithubAsset(
                        id: Int,
                        nodeId: String,
                        url: String,
                        name: String,
                        label: Option[String],
                        uploader: GithubUser,
                        size: Int,
                        downloadCount: Int,
                        createdAt: Date,
                        updatedAt: Date,
                        browserDownloadUrl: String
                      )