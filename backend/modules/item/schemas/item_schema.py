class CreateItemSchema:
    """
    Validates input for item creation.

    Required fields: cart_type_id, name, price
    Optional fields: description, image_urls, is_available
    """

    def __init__(self, data: dict):
        self._data = data
        self.errors = []
        self.validated_data = {}

    def is_valid(self) -> bool:
        self.errors = []
        self.validated_data = {}

        # cart_type_id — required, positive int
        cart_type_id = self._data.get("cart_type_id")
        if (
            cart_type_id is None
            or not isinstance(cart_type_id, int)
            or cart_type_id <= 0
        ):
            self.errors.append(
                "'cart_type_id' is required and must be a positive integer."
            )
        else:
            self.validated_data["cart_type_id"] = cart_type_id

        # name — required, non-empty string
        name = self._data.get("name")
        if not name or not isinstance(name, str) or not name.strip():
            self.errors.append("'name' is required and must be a non-empty string.")
        else:
            self.validated_data["name"] = name.strip()

        # description — optional, defaults to ""
        description = self._data.get("description", "")
        if not isinstance(description, str):
            self.errors.append("'description' must be a string.")
        else:
            self.validated_data["description"] = description.strip()

        # price — required, numeric, >= 0
        price = self._data.get("price")
        if price is None:
            self.errors.append("'price' is required.")
        elif not isinstance(price, (int, float)):
            self.errors.append("'price' must be a number.")
        elif price < 0:
            self.errors.append("'price' must be greater than or equal to 0.")
        else:
            self.validated_data["price"] = float(price)

        # image_url — optional, single URL string
        image_url = self._data.get("image_url")
        if image_url is not None:
            if not isinstance(image_url, str):
                self.errors.append("'image_url' must be a string.")
            elif len(image_url.strip()) == 0:
                self.errors.append("'image_url' cannot be empty.")
            else:
                self.validated_data["image_url"] = image_url.strip()
        else:
            self.validated_data["image_url"] = None

        # image_urls — optional, list of valid URL strings
        image_urls = self._data.get("image_urls")
        if image_urls is not None:
            if not isinstance(image_urls, list):
                self.errors.append("'image_urls' must be a list.")
            else:
                for i, url in enumerate(image_urls):
                    if not isinstance(url, str) or not url.strip():
                        self.errors.append(
                            f"'image_urls[{i}]' must be a non-empty string."
                        )
                    elif not (
                        url.strip().startswith("http://")
                        or url.strip().startswith("https://")
                    ):
                        self.errors.append(
                            f"'image_urls[{i}]' must be a valid URL starting with http:// or https://."
                        )
                if not self.errors or all("image_urls" not in e for e in self.errors):
                    self.validated_data["image_urls"] = [u.strip() for u in image_urls]
                else:
                    # Only set if no image_urls errors were added
                    pass
        else:
            self.validated_data["image_urls"] = []

        # is_available — optional, defaults to True
        is_available = self._data.get("is_available", True)
        if not isinstance(is_available, bool):
            self.errors.append("'is_available' must be a boolean.")
        else:
            self.validated_data["is_available"] = is_available

        return len(self.errors) == 0


class UpdateItemSchema:
    """
    Validates input for item updates.

    All fields are optional. Only provided fields are validated and returned.
    """

    def __init__(self, data: dict):
        self._data = data
        self.errors = []
        self.validated_data = {}

    def is_valid(self) -> bool:
        self.errors = []
        self.validated_data = {}

        if "cart_type_id" in self._data:
            cart_type_id = self._data["cart_type_id"]
            if not isinstance(cart_type_id, int) or cart_type_id <= 0:
                self.errors.append("'cart_type_id' must be a positive integer.")
            else:
                self.validated_data["cart_type_id"] = cart_type_id

        if "name" in self._data:
            name = self._data["name"]
            if not isinstance(name, str) or not name.strip():
                self.errors.append("'name' must be a non-empty string.")
            else:
                self.validated_data["name"] = name.strip()

        if "description" in self._data:
            description = self._data["description"]
            if not isinstance(description, str):
                self.errors.append("'description' must be a string.")
            else:
                self.validated_data["description"] = description.strip()

        if "price" in self._data:
            price = self._data["price"]
            if not isinstance(price, (int, float)):
                self.errors.append("'price' must be a number.")
            elif price < 0:
                self.errors.append("'price' must be greater than or equal to 0.")
            else:
                self.validated_data["price"] = float(price)

        if "image_url" in self._data:
            image_url = self._data["image_url"]
            if image_url is not None:
                if not isinstance(image_url, str):
                    self.errors.append("'image_url' must be a string.")
                elif len(image_url.strip()) == 0:
                    self.errors.append("'image_url' cannot be empty.")
                else:
                    self.validated_data["image_url"] = image_url.strip()
            else:
                self.validated_data["image_url"] = None

        if "image_urls" in self._data:
            image_urls = self._data["image_urls"]
            if not isinstance(image_urls, list):
                self.errors.append("'image_urls' must be a list.")
            else:
                for i, url in enumerate(image_urls):
                    if not isinstance(url, str) or not url.strip():
                        self.errors.append(
                            f"'image_urls[{i}]' must be a non-empty string."
                        )
                    elif not (
                        url.strip().startswith("http://")
                        or url.strip().startswith("https://")
                    ):
                        self.errors.append(
                            f"'image_urls[{i}]' must be a valid URL starting with http:// or https://."
                        )
                if not any("image_urls" in e for e in self.errors):
                    self.validated_data["image_urls"] = [u.strip() for u in image_urls]

        if "is_available" in self._data:
            is_available = self._data["is_available"]
            if not isinstance(is_available, bool):
                self.errors.append("'is_available' must be a boolean.")
            else:
                self.validated_data["is_available"] = is_available

        return len(self.errors) == 0
