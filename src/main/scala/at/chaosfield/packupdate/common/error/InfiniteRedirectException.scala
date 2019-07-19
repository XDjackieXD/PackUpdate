package at.chaosfield.packupdate.common.error

class InfiniteRedirectException(count: Int) extends Exception(s"Too many redirects (more than $count)")
