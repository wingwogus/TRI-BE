import ReactMarkdown from "react-markdown";
import { ArrowRight, CheckCircle2, ListPlus, MapPin, Sparkles, Star } from "lucide-react";
import type { TripReview } from "@/api/reviews";
import type { PlaceSearchResult } from "@/api/places";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { getPlacePhotoUrl, getPlaceTypeLabel } from "@/lib/placePresentation";
import { TripReviewRouteOptimizationSection } from "@/components/TripReviewRouteOptimizationSection";

export function TripReviewContent({ review, tripId, open, onAddWishlist, onAddItinerary, isAddingWishlist, isAddingItinerary }: {
  review: TripReview; tripId: number; open: boolean;
  onAddWishlist: (place: PlaceSearchResult) => void; onAddItinerary: (place: PlaceSearchResult) => void;
  isAddingWishlist: boolean; isAddingItinerary: boolean;
}) {
  return (
    <div className="space-y-6 animate-fade-in">
      {review.concept && (
        <div className="relative overflow-hidden rounded-lg bg-gradient-to-br from-primary/10 via-primary/5 to-transparent border border-primary/20 p-6">
          <div className="relative flex items-start gap-3">
            <div className="p-2 rounded-lg bg-primary/10">
              <Sparkles className="w-5 h-5 text-primary" />
            </div>
            <div className="flex-1">
              <p className="text-sm font-medium text-muted-foreground mb-1">분석 콘셉트</p>
              <p className="text-lg font-semibold text-foreground">{review.concept}</p>
            </div>
          </div>
        </div>
      )}

      <div className="prose prose-sm dark:prose-invert max-w-none">
        <ReactMarkdown
          components={{
            h2: ({ children }) => (
              <h2 className="text-2xl font-bold mt-8 mb-4 text-foreground flex items-center gap-2 pb-2 border-b border-border/50">
                <CheckCircle2 className="w-5 h-5 text-primary" />
                {children}
              </h2>
            ),
            h3: ({ children }) => (
              <h3 className="text-xl font-semibold mt-6 mb-3 text-foreground flex items-center gap-2">
                <ArrowRight className="w-4 h-4 text-primary" />
                {children}
              </h3>
            ),
            p: ({ children }) => (
              <p className="mb-4 text-foreground/90 leading-relaxed text-[15px]">{children}</p>
            ),
            ul: ({ children }) => (
              <ul className="mb-4 space-y-2 text-foreground/90">{children}</ul>
            ),
            li: ({ children }) => (
              <li className="flex items-start gap-2 ml-4">
                <span className="text-primary mt-1.5">•</span>
                <span className="flex-1">{children}</span>
              </li>
            ),
            ol: ({ children }) => (
              <ol className="mb-4 space-y-2 text-foreground/90 list-decimal list-inside">{children}</ol>
            ),
            strong: ({ children }) => (
              <strong className="font-semibold text-foreground bg-primary/10 px-1 rounded">{children}</strong>
            ),
            a: ({ href, children }) => (
              <a
                href={href}
                target="_blank"
                rel="noopener noreferrer"
                className="text-primary hover:underline inline-flex items-center gap-1 font-medium"
              >
                {children}
              </a>
            ),
          }}
        >
          {review.content}
        </ReactMarkdown>
      </div>

      <TripReviewRouteOptimizationSection key={`${tripId}:${review.reviewId}`} tripId={tripId} open={open} optimization={review.routeOptimization} />

      {/* 추천 장소 섹션 */}
      {review.recommendedPlaces && review.recommendedPlaces.length > 0 && (
        <div className="mt-8 pt-6 border-t">
          <div className="flex items-center gap-2 mb-4">
            <MapPin className="w-5 h-5 text-primary" />
            <h3 className="text-xl font-bold text-foreground">AI 추천 장소</h3>
          </div>
          <div className="grid gap-3">
            {review.recommendedPlaces.map((place, index) => (
              <Card key={`${place.externalPlaceId}-${index}`} className="overflow-hidden border-2 hover:border-primary/30 transition-all">
                <CardContent className="p-4">
                  <div className="flex flex-col items-start justify-between gap-3 sm:flex-row">
                    <div className="flex flex-1 min-w-0 gap-3">
                      {getPlacePhotoUrl(place.photoHint) && (
                        <img
                          src={getPlacePhotoUrl(place.photoHint) || undefined}
                          alt={place.placeName}
                          className="h-16 w-16 rounded-lg object-cover border shrink-0"
                        />
                      )}
                      <div className="flex-1 min-w-0">
                        <h4 className="font-semibold text-foreground mb-1 flex items-center gap-2">
                          <Star className="w-4 h-4 text-amber-500 flex-shrink-0" />
                          <span className="truncate">{place.placeName}</span>
                        </h4>
                        <div className="flex flex-wrap gap-2 mb-2">
                          {getPlaceTypeLabel(place.placeTypeSummary, place.normalizedCategoryKey) && (
                            <Badge variant="secondary">{getPlaceTypeLabel(place.placeTypeSummary, place.normalizedCategoryKey)}</Badge>
                          )}
                          {typeof place.placeDetailSummary?.rating === "number" && (
                            <Badge variant="outline">평점 {place.placeDetailSummary.rating.toFixed(1)}</Badge>
                          )}
                        </div>
                        <p className="text-sm text-muted-foreground truncate">{place.address}</p>
                        {place.placeDetailSummary?.editorialSummary && (
                          <p className="text-xs text-muted-foreground mt-1 line-clamp-2">
                            {place.placeDetailSummary.editorialSummary}
                          </p>
                        )}
                      </div>
                    </div>
                    <div className="flex flex-wrap gap-2 flex-shrink-0">
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => onAddWishlist(place)}
                        disabled={isAddingWishlist}
                        className="whitespace-nowrap"
                      >
                        <Star className="w-3.5 h-3.5 mr-1" />
                        위시리스트
                      </Button>
                      <Button
                        size="sm"
                        onClick={() => onAddItinerary(place)}
                        disabled={isAddingItinerary}
                        className="whitespace-nowrap"
                      >
                        <ListPlus className="w-3.5 h-3.5 mr-1" />
                        일정 추가
                      </Button>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
