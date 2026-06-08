from pydantic import BaseModel, Field


class CreateSocietySchema(BaseModel):
    name: str = Field(min_length=1, max_length=100)
    description: str | None = None
    region_id: int
    sport_id: int
    is_public: bool = True
    max_members: int = Field(default=50, ge=2, le=500)
    owner_user_id: int | None = None


class UpdateSocietySchema(BaseModel):
    name: str | None = Field(default=None, min_length=1, max_length=100)
    description: str | None = None
    is_public: bool | None = None
    max_members: int | None = Field(default=None, ge=2, le=500)
