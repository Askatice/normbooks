import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.google.gson.Gson
import java.net.URL
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import java.awt.Desktop
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.util.*
import javax.imageio.ImageIO


data class Book(val id: String, val title: String, val author: String, val imageUrl: String, val bookUrl: String)

fun main() = application {
    Window(
        title = "Книги",
        onCloseRequest = ::exitApplication
    ) {

        MaterialTheme {
            Surface(
                modifier = Modifier
                    .wrapContentSize()
            ) {
                Column(
                    modifier = Modifier
                        .wrapContentSize()
                ) {
                    val text = remember {
                        mutableStateOf("")
                    }
                    val books = remember { mutableStateOf(emptyList<Book>()) }
                    searchBar(books, text)
                    Button(onClick = {
                        val json = URL("https://www.googleapis.com/books/v1/volumes?q=book&maxResults=30").readText()
                        val response = Gson().fromJson(json, GoogleBooksResponse::class.java)
                        books.value = response.items.mapNotNull { item ->
                            try {
                                val volumeInfo = item.volumeInfo
                                Book(
                                    id = item.id,
                                    title = volumeInfo.title,
                                    author = volumeInfo.authors?.firstOrNull() ?: "",
                                    imageUrl = volumeInfo.imageLinks?.thumbnail ?: "",
                                    bookUrl = item.previewLink?.previewLink ?: "",

                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                    }) {
                        Text("Загрузить книги")
                    }
                    Row {
                        val bookItem: List<Book> = books.component1()
                        VerticalScrollbar(

                            modifier = Modifier.fillMaxHeight()
                                .width(10.dp)
                                .background(Color.Black),
                            adapter = rememberScrollbarAdapter(
                                scrollState = rememberLazyListState(bookItem.size),
                            ),

                            )

                        books.value.forEach { _ ->
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(160.dp),
                                contentPadding = PaddingValues(40.dp)
                            ) {
                                itemsIndexed(bookItem) { _, book ->
                                    itemForLazy(book, book.id)
                                }
                            }
                        }
                    }

                }
            }
        }
    }
}

data class GoogleBooksResponse(val items: List<Item>)
data class Item(val id: String, val volumeInfo: VolumeInfo, val previewLink: BookLinks?)
data class VolumeInfo(val title: String, val authors: List<String>?, val imageLinks: ImageLinks?)
data class ImageLinks(val thumbnail: String)
data class BookLinks(val previewLink: String)


fun loadNetworkImageDesktop(link: String): ImageBitmap {
    val url = URL(link)
    val connection = url.openConnection() as HttpURLConnection
    connection.connect()

    val inputStream = connection.inputStream
    val bufferedImage = ImageIO.read(inputStream)

    val stream = ByteArrayOutputStream()
    ImageIO.write(bufferedImage, "png", stream)
    val byteArray = stream.toByteArray()

    return org.jetbrains.skia.Image.makeFromEncoded(byteArray).toComposeImageBitmap()
}

@Composable
fun itemForLazy(book: Book, uri: String) {
    Card(elevation = 8.dp,
        modifier = Modifier
            .padding(4.dp)
            .clickable {
                openInBrowser(uri)
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Image(
                bitmap = loadNetworkImageDesktop(
                    book.imageUrl
                ), ""
            )
            Column {
                Text(text = book.title)
                Text(text = book.author, style = MaterialTheme.typography.caption)
            }
        }
    }
}

@Composable
fun searchBar(books: MutableState<List<Book>>, text: MutableState<String>){
    TextField(
        modifier = Modifier
            .fillMaxWidth(),
        value = text.value,
        onValueChange = {
            text.value = it
        },
        placeholder = {
            Text(
                modifier = Modifier
                    .alpha(ContentAlpha.medium),
                text = "Search here...",
                color = Color.Black
            )

        },
        textStyle = TextStyle(
            fontSize = MaterialTheme.typography.subtitle1.fontSize
        ),
        singleLine = true,
        leadingIcon = {
            IconButton(
                modifier = Modifier
                    .alpha(ContentAlpha.medium),
                onClick = {
                    val json = URL("https://www.googleapis.com/books/v1/volumes?q=${text.value}&maxResults=30").readText()
                    val response = Gson().fromJson(json, GoogleBooksResponse::class.java)
                    books.value = response.items.mapNotNull { item ->
                        try {
                            val volumeInfo = item.volumeInfo
                            Book(
                                id = item.id,
                                title = volumeInfo.title,
                                author = volumeInfo.authors?.firstOrNull() ?: "",
                                imageUrl = volumeInfo.imageLinks?.thumbnail ?: "",
                                bookUrl = item.previewLink?.previewLink ?: "",

                                )
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "",
                    tint = Color.Black
                )
            }
        },
        trailingIcon = {
            IconButton(onClick = {
                if (text.value.isNotEmpty()) {
                    text.value = ""
                } else {
//                                    onCloseClicked()
                }
            }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "",
                    tint = Color.Black
                )
            }
        },
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search,
        ),
        keyboardActions = KeyboardActions(
            onSearch = {
                val json = URL("https://www.googleapis.com/books/v1/volumes?q=${text.value}&maxResults=30").readText()
                val response = Gson().fromJson(json, GoogleBooksResponse::class.java)
                books.value = response.items.mapNotNull { item ->
                    try {
                        val volumeInfo = item.volumeInfo
                        Book(
                            id = item.id,
                            title = volumeInfo.title,
                            author = volumeInfo.authors?.firstOrNull() ?: "",
                            imageUrl = volumeInfo.imageLinks?.thumbnail ?: "",
                            bookUrl = item.previewLink?.previewLink ?: "",

                            )
                    } catch (e: Exception) {
                        null
                    }
                }
            }
        ),
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = Color.Transparent,
            cursorColor = Color.Black.copy(alpha = ContentAlpha.medium)
        )

    )
}

fun openInBrowser(id: String) {
    val osName by lazy(LazyThreadSafetyMode.NONE) { System.getProperty("os.name").lowercase(Locale.getDefault()) }
    val desktop = Desktop.getDesktop()
    when {
        Desktop.isDesktopSupported() && desktop.isSupported(Desktop.Action.BROWSE) -> desktop.browse(URI("http://books.google.kg/books?id=$id&printsec=frontcover&dq=book&hl=&cd=1&source=gbs_api"))
        else -> throw RuntimeException("cannot open")
    }
}



