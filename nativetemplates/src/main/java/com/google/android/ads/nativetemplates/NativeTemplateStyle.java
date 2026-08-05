// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.android.ads.nativetemplates;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;

/**
 * A class containing the optional styling options for the Native Template. *
 */
public class NativeTemplateStyle {

	@ColorInt
	private int primaryTextColor;

	@ColorInt
	private int secondaryTextColor;

	@DrawableRes
	private int backgroundResources;

	@DrawableRes
	private int callToActionBackgroundResource;

	@DrawableRes
	private int iconBackgroundResource;

	@DrawableRes
	private int templateViewBackgroundResource;

	public int getPrimaryTextColor() {
		return primaryTextColor;
	}

	public void setPrimaryTextColor(@ColorInt int primaryTextColor) {
		this.primaryTextColor = primaryTextColor;
	}

	public int getSecondaryTextColor() {
		return secondaryTextColor;
	}

	public void setSecondaryTextColor(@ColorInt int secondaryTextColor) {
		this.secondaryTextColor = secondaryTextColor;
	}

	public int getBackgroundResources() {
		return backgroundResources;
	}

	public void setBackgroundResources(@DrawableRes int backgroundResources) {
		this.backgroundResources = backgroundResources;
	}

	public int getCallToActionBackgroundResource() {
		return callToActionBackgroundResource;
	}

	public void setCallToActionBackgroundResource(
		@DrawableRes int callToActionBackgroundResource
	) {
		this.callToActionBackgroundResource = callToActionBackgroundResource;
	}

	public int getIconBackgroundResource() {
		return iconBackgroundResource;
	}

	public void setIconBackgroundResource(@DrawableRes int iconBackgroundResource) {
		this.iconBackgroundResource = iconBackgroundResource;
	}

	public int getTemplateViewBackgroundResource() {
		return templateViewBackgroundResource;
	}

	public void setTemplateViewBackgroundResource(
		@DrawableRes int templateViewBackgroundResource
	) {
		this.templateViewBackgroundResource = templateViewBackgroundResource;
	}

	/**
	 * A class that provides helper methods to build a style object. *
	 */
	public static class Builder {

		private NativeTemplateStyle style;

		public Builder() {
			this.style = new NativeTemplateStyle();
		}

		public Builder setPrimaryTextColor(@ColorInt int primaryTextColor) {
			this.style.setPrimaryTextColor(primaryTextColor);
			return this;
		}

		public Builder setSecondaryTextColor(@ColorInt int secondaryTextColor) {
			this.style.setSecondaryTextColor(secondaryTextColor);
			return this;
		}

		public Builder setBackgroundResource(@DrawableRes int backgroundResource) {
			this.style.setBackgroundResources(backgroundResource);
			return this;
		}

		public Builder setCallToActionBackgroundResource(
			@DrawableRes int callToActionBackgroundResource
		) {
			this.style.setCallToActionBackgroundResource(callToActionBackgroundResource);
			return this;
		}

		public Builder setIconBackgroundResource(
			@DrawableRes int iconBackgroundResource
		) {
			this.style.setIconBackgroundResource(iconBackgroundResource);
			return this;
		}

		public Builder setTemplateViewBackgroundResource(
			@DrawableRes int templateViewBackgroundResource
		) {
			this.style.setTemplateViewBackgroundResource(templateViewBackgroundResource);
			return this;
		}

		public NativeTemplateStyle build() {
			return style;
		}
	}
}
