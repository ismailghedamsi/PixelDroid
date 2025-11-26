package org.pixeldroid.app.posts.feeds.uncachedFeeds.profile

import androidx.paging.PagingSource
import androidx.paging.PagingState
import org.pixeldroid.app.utils.api.PixelfedAPI
import org.pixeldroid.app.utils.api.objects.Collection

class CollectionsPagingSource(
    private val api: PixelfedAPI,
    private val accountId: String,
) : PagingSource<Int, Collection>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Collection> {
        return try {
            val page = params.key ?: 1
            val collections = api.accountCollections(accountId, page)

            LoadResult.Page(
                data = collections,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (collections.isEmpty()) null else page + 1
            )
        } catch (exception: Exception) {
            LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Collection>): Int? {
        return state.anchorPosition?.let { anchor ->
            val anchorPage = state.closestPageToPosition(anchor)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}