package at.chaosfield.packupdate.json

import java.net.URI
import java.util.Date

case class GithubRelease(
                          id: Int,
                          url: URI,
                          assetsUrl: URI,
                          htmlUrl: URI,
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
                          tarballUrl: URI,
                          zipballUrl: URI,
                          body: String
                        )

case class GithubUser(
                      id: Int,
                      login: String,
                      nodeId: String,
                      avatarUrl: String,
                      gravatarId: String,
                      url: URI,
                      htmlUrl: URI,
                      reposUrl: URI,
                      `type`: String,
                      siteAdmin: Boolean
                     )

case class GithubAsset(
                        id: Int,
                        nodeId: String,
                        url: URI,
                        name: String,
                        label: Option[String],
                        uploader: GithubUser,
                        size: Int,
                        downloadCount: Int,
                        createdAt: Date,
                        updatedAt: Date,
                        browserDownloadUrl: URI
                      )